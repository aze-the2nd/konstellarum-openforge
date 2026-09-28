package de.konstellarum.synesis.core.sensor

/**
 * Tries candidate endpoints in order and returns the first non-null result.
 * Used by repositories that should fall back from a preferred address
 * (e.g. the LAN address of the bridge) to a secondary one (the tailnet address).
 */
object EndpointFallback {

    suspend fun <T> firstSuccess(
        candidates: List<String>,
        attempt: suspend (String) -> T?,
    ): T? {
        for (candidate in candidates) {
            val result = attempt(candidate)
            if (result != null) return result
        }
        return null
    }
}
