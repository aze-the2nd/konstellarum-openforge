package de.konstellarum.synesis.core.sensor

/**
 * Min/average/max over a sample window. Empty input yields null.
 */
object TempStats {

    fun compute(samples: List<TempSample>): TempStatistics? {
        if (samples.isEmpty()) return null
        val min = samples.minOf { it.tempC }
        val max = samples.maxOf { it.tempC }
        val avg = samples.sumOf { it.tempC } / samples.size
        return TempStatistics(min = min, avg = avg, max = max)
    }
}
