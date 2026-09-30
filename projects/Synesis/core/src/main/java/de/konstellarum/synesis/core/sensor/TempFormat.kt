package de.konstellarum.synesis.core.sensor

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * German display formatting for temperatures (decimal comma, one decimal, half-up)
 * and timestamps (day/month, hour/minute). Axis labels use shorter variants:
 * intraday windows show clock time, longer windows the date.
 */
object TempFormat {

    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM. HH:mm")

    private val timeOfDayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.")

    private fun decimal(value: Double): String =
        BigDecimal.valueOf(value)
            .setScale(1, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',')

    fun celsius(value: Double): String = decimal(value) + " °C"

    /** Compact axis label, e.g. "14,5°". */
    fun axisCelsius(value: Double): String = decimal(value) + "°"

    fun timestamp(unixSeconds: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochSecond(unixSeconds).atZone(zone).format(timestampFormatter)

    /**
     * Absolute axis label: "HH:mm" for spans up to 48 h (clock time is what matters),
     * "dd.MM." for longer spans.
     */
    fun axisTime(unixSeconds: Long, spanSec: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val formatter = if (spanSec <= 48 * 60 * 60) timeOfDayFormatter else dateFormatter
        return Instant.ofEpochSecond(unixSeconds).atZone(zone).format(formatter)
    }

    /** Visible-window caption, e.g. "26.09. 14:32 – 27.09. 14:32". */
    fun rangeLabel(startSec: Long, endSec: Long): String =
        timestamp(startSec) + " – " + timestamp(endSec)
}
