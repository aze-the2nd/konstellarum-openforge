package de.konstellarum.synesis.core.sensor

import kotlin.math.ceil
import kotlin.math.floor

/**
 * A visible time span of the chart, in unix seconds (inclusive start, inclusive end).
 */
data class ChartWindow(
    val startSec: Long,
    val endSec: Long,
) {
    val spanSec: Long get() = endSec - startSec
}

/**
 * Quick-select ranges for the chart's X axis. Windows are anchored at "now" (or at the
 * newest sample when the series lags behind) and clamped to the available data extent.
 */
enum class TimeRangePreset(val label: String) {
    LAST_HOUR("1 h"),
    LAST_24_HOURS("24 h"),
    LAST_7_DAYS("7 d"),
    ALL("Alle"),
    ;

    fun window(nowSec: Long, dataMinSec: Long, dataMaxSec: Long): ChartWindow {
        val end = minOf(nowSec, dataMaxSec)
        val start = when (this) {
            LAST_HOUR -> nowSec - 60 * 60
            LAST_24_HOURS -> nowSec - 24 * 60 * 60
            LAST_7_DAYS -> nowSec - 7L * 24 * 60 * 60
            ALL -> dataMinSec
        }
        return ChartWindow(start.coerceIn(dataMinSec, end), end)
    }
}

/**
 * Y axis bounds for a temperature series: nice multiples of a sensible step so the
 * scale is labeled with round values (whole degrees or half degrees).
 */
data class TemperatureAxisBounds(
    val lo: Double,
    val hi: Double,
    val step: Double,
)

/**
 * Pure helpers for the temperature chart: window clamping (pinch/pan), Y-axis bounds and
 * X-axis ticks. Everything here is JVM-tested; the Compose canvas only renders the results.
 */
object ChartWindowLogic {

    const val MIN_WINDOW_SEC = 5 * 60L

    fun temperatureBounds(min: Double, max: Double): TemperatureAxisBounds {
        val span = max - min
        val step = when {
            span >= 6.0 -> 2.0
            span >= 3.0 -> 1.0
            else -> 0.5
        }
        val lo = floor((min - 0.25 * step) / step) * step
        val hi = ceil((max + 0.25 * step) / step) * step
        return TemperatureAxisBounds(lo = lo, hi = hi, step = step)
    }

    /**
     * Clamps a requested window (from pinch/pan) to the data extent, keeping the span
     * within [MIN_WINDOW_SEC, extent] and fully inside [dataMinSec, dataMaxSec].
     */
    fun clampWindow(
        requested: ChartWindow,
        dataMinSec: Long,
        dataMaxSec: Long,
    ): ChartWindow {
        val extent = dataMaxSec - dataMinSec
        if (extent <= 0) return ChartWindow(dataMinSec, dataMaxSec)
        val maxSpan = maxOf(extent, 1L)
        val span = requested.spanSec.coerceIn(minOf(MIN_WINDOW_SEC, maxSpan), maxSpan)
        val start = requested.startSec.coerceIn(dataMinSec, dataMaxSec - span)
        return ChartWindow(start, start + span)
    }

    /**
     * A tick interval (seconds) that yields 4–10 readable labels for the given span.
     */
    fun timeTickIntervalSec(spanSec: Long): Long {
        val span = maxOf(spanSec, 60L)
        return when {
            span <= 2 * 60 * 60 -> 15 * 60
            span <= 12 * 60 * 60 -> 60 * 60
            span <= 48 * 60 * 60 -> 3 * 60 * 60
            span <= 7L * 24 * 60 * 60 -> 12 * 60 * 60
            span <= 31L * 24 * 60 * 60 -> 24 * 60 * 60
            else -> 7L * 24 * 60 * 60
        }
    }

    /**
     * Absolute tick positions within [startSec, endSec], aligned to epoch multiples of the
     * chosen interval so labels fall on round clock times.
     */
    fun timeTicks(startSec: Long, endSec: Long): List<Long> {
        if (endSec <= startSec) return emptyList()
        val interval = timeTickIntervalSec(endSec - startSec)
        val first = ((startSec + interval - 1) / interval) * interval
        return generateSequence(first) { it + interval }.takeWhile { it <= endSec }.toList()
    }
}
