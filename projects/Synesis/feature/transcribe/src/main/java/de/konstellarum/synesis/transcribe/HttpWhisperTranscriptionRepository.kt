package de.konstellarum.synesis.transcribe

import de.konstellarum.synesis.core.domain.WhisperTranscriptionRepository
import de.konstellarum.synesis.core.sensor.EndpointFallback
import de.konstellarum.synesis.core.transcribe.WhisperTranscriptionContract
import de.konstellarum.synesis.core.transcribe.WhisperTranscriptionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uploads the raw audio recording to the private Whisper endpoint on the bridge
 * (LAN first, tailnet as fallback). The app embeds no model or API credentials;
 * the concrete Whisper model stays server-side.
 */
class HttpWhisperTranscriptionRepository(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val lanBaseUrl: String = LAN_BASE_URL,
) : WhisperTranscriptionRepository {

    private val candidates: List<String>
        get() = listOfNotNull(lanBaseUrl.takeIf { it.isNotBlank() }, baseUrl)

    override suspend fun transcribe(audioFile: File): WhisperTranscriptionResult {
        val bytes = withContext(Dispatchers.IO) {
            if (!audioFile.isFile) return@withContext ByteArray(0)
            audioFile.readBytes()
        }
        if (bytes.isEmpty()) {
            return WhisperTranscriptionResult.Failure("Audio-Datei ist leer oder fehlt")
        }
        if (bytes.size > WhisperTranscriptionContract.MAX_AUDIO_BYTES) {
            return WhisperTranscriptionResult.Failure("Audio-Datei ist zu groß (max. 25 MB)")
        }

        return withContext(Dispatchers.IO) {
            var lastFailure: WhisperTranscriptionResult.Failure? = null
            val result = EndpointFallback.firstSuccess(candidates) { candidate ->
                when (val attempt = attemptUpload(candidate, bytes)) {
                    is WhisperTranscriptionResult.Success -> attempt
                    is WhisperTranscriptionResult.Failure -> {
                        lastFailure = attempt
                        null
                    }
                }
            }
            result ?: lastFailure ?: WhisperTranscriptionResult.Failure("Whisper-Endpunkt nicht erreichbar")
        }
    }

    private fun attemptUpload(base: String, audioBytes: ByteArray): WhisperTranscriptionResult {
        val endpoint = "${base.trimEnd('/')}${WhisperTranscriptionContract.ENDPOINT_PATH}"
        return try {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                // Whisper on the bridge CPU can take a while for longer dictations.
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", WhisperTranscriptionContract.CONTENT_TYPE)
                setRequestProperty("Accept", "application/json")
                setFixedLengthStreamingMode(audioBytes.size)
            }
            try {
                connection.outputStream.use { output -> output.write(audioBytes) }
                val responseCode = connection.responseCode
                val body = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
                if (responseCode in 200..299) {
                    WhisperTranscriptionContract.parseResponse(body)
                } else {
                    WhisperTranscriptionResult.Failure("Whisper-Endpunkt antwortet mit HTTP $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            WhisperTranscriptionResult.Failure(exception.message ?: "Whisper-Verbindung fehlgeschlagen")
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://100.101.80.34:5005"
        const val LAN_BASE_URL = "http://192.168.178.23:5005"
        const val CONNECT_TIMEOUT_MS = 4_000
        const val READ_TIMEOUT_MS = 300_000
    }
}
