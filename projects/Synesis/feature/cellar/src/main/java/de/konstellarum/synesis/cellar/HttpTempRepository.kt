package de.konstellarum.synesis.cellar

import android.content.Context
import de.konstellarum.synesis.core.sensor.EndpointFallback
import de.konstellarum.synesis.core.sensor.FetchStatus
import de.konstellarum.synesis.core.sensor.SensorResponseParser
import de.konstellarum.synesis.core.sensor.TempRepository
import de.konstellarum.synesis.core.sensor.TempSample
import de.konstellarum.synesis.core.store.FileBackedListStore
import de.konstellarum.synesis.core.store.JsonFileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Reads the keller_temp series from the iot-db-bridge. The LAN address of the
 * bridge is tried first (reachable from the home WLAN without Tailscale),
 * then the tailnet address as fallback. Fetched samples replace the local
 * cache (bounded to [MAX_HISTORY]) so the chart survives restarts.
 */
class HttpTempRepository(
    context: Context,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val lanBaseUrl: String = LAN_BASE_URL,
) : TempRepository {

    private val store = FileBackedListStore(
        JsonFileStore.of<List<TempSample>>(File(context.filesDir, CACHE_FILE_NAME)),
    )

    private val _status = MutableStateFlow<FetchStatus>(FetchStatus.Idle)
    override val status: StateFlow<FetchStatus> = _status.asStateFlow()

    override val samples: StateFlow<List<TempSample>> = store.items

    private val candidates: List<String>
        get() = listOfNotNull(lanBaseUrl.takeIf { it.isNotBlank() }, baseUrl)

    override suspend fun fetch() {
        _status.value = FetchStatus.Loading
        val result = withContext(Dispatchers.IO) { downloadSamples() }
        when (result) {
            is SensorResponseParser.ParseResult.Success -> {
                store.mutate { result.samples.takeLast(MAX_HISTORY) }
                _status.value = FetchStatus.Success(System.currentTimeMillis())
            }

            is SensorResponseParser.ParseResult.Failure -> {
                _status.value = FetchStatus.Error(result.message)
            }
        }
    }

    private suspend fun downloadSamples(): SensorResponseParser.ParseResult {
        var lastFailure: SensorResponseParser.ParseResult.Failure? = null
        val result = EndpointFallback.firstSuccess(candidates) { candidate ->
            when (val attempt = attemptDownload(candidate)) {
                is SensorResponseParser.ParseResult.Success -> attempt
                is SensorResponseParser.ParseResult.Failure -> {
                    lastFailure = attempt
                    null
                }
            }
        }
        return result ?: lastFailure
            ?: SensorResponseParser.ParseResult.Failure("Kein Endpunkt erreichbar")
    }

    private fun attemptDownload(base: String): SensorResponseParser.ParseResult {
        return try {
            val endpoint = URL("$base/keller_temp?limit=$FETCH_LIMIT")
            val connection = endpoint.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            try {
                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    SensorResponseParser.ParseResult.Failure("Sensor antwortet mit HTTP $responseCode")
                } else {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    SensorResponseParser.parse(body)
                }
            } finally {
                connection.disconnect()
            }
        } catch (throwable: Throwable) {
            SensorResponseParser.ParseResult.Failure(throwable.message ?: "Verbindung fehlgeschlagen")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://100.101.80.34:5005"

        /**
         * LAN address of the bridge on the Pi (home WLAN, no Tailscale needed).
         * Confirmed by Aurora on 2026-09-28; the LAN endpoint is tried first and
         * the tailnet endpoint stays as fallback.
         */
        const val LAN_BASE_URL = "http://192.168.178.23:5005"

        const val FETCH_LIMIT = 500
        const val MAX_HISTORY = 2000
        const val CACHE_FILE_NAME = "cellar-samples.json"
    }
}
