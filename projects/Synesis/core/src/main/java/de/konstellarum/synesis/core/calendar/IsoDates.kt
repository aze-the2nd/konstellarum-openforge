package de.konstellarum.synesis.core.calendar

import java.time.LocalDate

/**
 * Conversion helpers between ISO-8601 date strings and the epoch-millis convention used by
 * the Material date picker (UTC calendar days).
 */
object IsoDates {

    const val MILLIS_PER_DAY = 86_400_000L

    fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date) }.getOrNull()

    fun toEpochMillis(date: LocalDate): Long = date.toEpochDay() * MILLIS_PER_DAY

    fun fromEpochMillis(millis: Long): LocalDate = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)
}
