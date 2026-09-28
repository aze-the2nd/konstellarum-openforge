package de.konstellarum.synesis.core.sensor

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * German display formatting for temperatures (decimal comma, one decimal, half-up)
 * and timestamps (day/month, hour/minute).
 */
object TempFormat {

    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM. HH:mm")

    fun celsius(value: Double): String =
        BigDecimal.valueOf(value)
            .setScale(1, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',') + " °C"

    fun timestamp(unixSeconds: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochSecond(unixSeconds).atZone(zone).format(timestampFormatter)
}
