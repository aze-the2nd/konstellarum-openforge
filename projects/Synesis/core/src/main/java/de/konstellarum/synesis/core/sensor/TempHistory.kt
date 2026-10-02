package de.konstellarum.synesis.core.sensor

/**
 * Pure helpers for the local temperature history. The merged history is kept in
 * ascending time order (oldest first, newest last) so consumers read the current
 * value as `samples.last()` — the invariant the cellar UI relies on.
 */
object TempHistory {

    /**
     * Merges freshly [fetched] samples into the [cached] history: deduplicated by
     * timestamp (the fetched sample wins), sorted ascending and capped to the newest
     * [maxHistory] samples.
     */
    fun merge(fetched: List<TempSample>, cached: List<TempSample>, maxHistory: Int): List<TempSample> =
        (fetched + cached)
            .distinctBy { it.t }
            .sortedBy { it.t }
            .takeLast(maxHistory.coerceAtLeast(0))
}
