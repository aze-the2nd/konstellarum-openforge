package de.konstellarum.synesis.core.sensor

import de.konstellarum.synesis.core.store.SynesisJson

/**
 * Parses the iot-db-bridge response. Samples are returned in ascending time order
 * regardless of the wire order.
 */
object SensorResponseParser {

    sealed interface ParseResult {
        data class Success(val samples: List<TempSample>) : ParseResult
        data class Failure(val message: String) : ParseResult
    }

    fun parse(json: String): ParseResult {
        val response = try {
            SynesisJson.codec.decodeFromString(SensorResponse.serializer(), json)
        } catch (_: Throwable) {
            return ParseResult.Failure("Antwort konnte nicht gelesen werden")
        }
        if (!response.ok) {
            return ParseResult.Failure(response.error ?: "Unbekannter Sensorfehler")
        }
        return ParseResult.Success(response.values.sortedBy { it.t })
    }
}
