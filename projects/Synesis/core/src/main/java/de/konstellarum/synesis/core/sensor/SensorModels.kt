package de.konstellarum.synesis.core.sensor

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One temperature reading. [t] is unix time in seconds, matching the bridge contract
 * (`keller_temp?since=&limit=` → values with `t`/`temp_c`).
 */
@Serializable
data class TempSample(
    val t: Long,
    @SerialName("temp_c") val tempC: Double,
)

/**
 * Wire format of the iot-db-bridge read endpoint.
 */
@Serializable
data class SensorResponse(
    val ok: Boolean = false,
    val count: Int = 0,
    val values: List<TempSample> = emptyList(),
    val error: String? = null,
)

data class TempStatistics(
    val min: Double,
    val avg: Double,
    val max: Double,
)

data class ChartPoint(
    val x: Float,
    val y: Float,
)

sealed interface FetchStatus {
    data object Idle : FetchStatus
    data object Loading : FetchStatus
    data class Success(val fetchedAtEpochMillis: Long) : FetchStatus
    data class Error(val message: String) : FetchStatus
}
