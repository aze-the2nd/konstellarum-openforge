package de.konstellarum.synesis.core.sensor

/**
 * Maps temperature samples onto canvas coordinates: time → x, temperature → y
 * (low at the bottom, high at the top). Long windows are downsampled to [maxPoints]
 * while preserving the first and last sample. A constant series is drawn in the
 * vertical middle; a single sample as a centered point.
 */
object ChartScaling {

    fun fit(
        samples: List<TempSample>,
        width: Float,
        height: Float,
        padding: Float,
        maxPoints: Int = 120,
    ): List<ChartPoint> {
        if (samples.isEmpty()) return emptyList()
        val reduced = downsample(samples, maxPoints)
        if (reduced.size == 1) {
            return listOf(ChartPoint(x = width / 2f, y = height / 2f))
        }
        val min = reduced.minOf { it.tempC }
        val max = reduced.maxOf { it.tempC }
        val innerWidth = width - 2 * padding
        val innerHeight = height - 2 * padding

        return reduced.mapIndexed { index, sample ->
            val x = padding + index.toFloat() / (reduced.size - 1) * innerWidth
            val y = if (max - min < 1e-9) {
                padding + innerHeight / 2f
            } else {
                val normalized = (sample.tempC - min).toFloat() / (max - min).toFloat()
                padding + (1f - normalized) * innerHeight
            }
            ChartPoint(x = x, y = y)
        }
    }

    private fun downsample(samples: List<TempSample>, maxPoints: Int): List<TempSample> {
        if (samples.size <= maxPoints) return samples
        val step = (samples.size - 1).toDouble() / (maxPoints - 1)
        return (0 until maxPoints).map { index -> samples[(index * step).toInt()] }
    }
}
