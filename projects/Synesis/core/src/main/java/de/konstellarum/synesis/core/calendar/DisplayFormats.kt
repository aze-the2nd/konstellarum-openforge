package de.konstellarum.synesis.core.calendar

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * German display strings for the calendar module. Weekday labels are fixed short forms;
 * the month title uses the German full month name.
 */
object DisplayFormats {

    private val monthTitleFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)

    fun monthTitle(yearMonth: YearMonth): String =
        yearMonth.atDay(1).format(monthTitleFormatter)

    val weekdayShortLabels: List<String> = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
}
