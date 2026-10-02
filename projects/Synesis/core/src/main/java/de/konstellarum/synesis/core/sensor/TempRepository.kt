package de.konstellarum.synesis.core.sensor

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for a temperature sensor source. Implementations fetch from a concrete
 * endpoint (e.g. the iot-db-bridge on the tailnet), keep a bounded sample history,
 * and surface the last fetch status.
 *
 * Invariant: [samples] is in ascending time order (oldest first, newest last), so the
 * current value is always `samples.last()`. Merge order is owned by [TempHistory].
 */
interface TempRepository {
    val samples: StateFlow<List<TempSample>>
    val status: StateFlow<FetchStatus>

    suspend fun fetch()
}
