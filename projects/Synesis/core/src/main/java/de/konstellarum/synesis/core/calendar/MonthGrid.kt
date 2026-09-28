package de.konstellarum.synesis.core.calendar

import java.time.DayOfWeek
import java.time.YearMonth

/**
 * One cell of the month grid. Null [dayOfMonth] and [date] mark padding cells before the
 * first and after the last day of the month.
 */
data class MonthCell(
    val dayOfMonth: Int?,
    val date: String?,
)

/**
 * Pure month-grid logic: a week starts on Monday and the grid is padded to full weeks.
 */
object MonthGrid {

    val weekDaysStartingMonday: List<DayOfWeek> = listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
    )

    fun cellsFor(yearMonth: YearMonth): List<MonthCell> {
        val firstOfMonth = yearMonth.atDay(1)
        val leadingBlanks = Math.floorMod(firstOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value, 7)

        val cells = mutableListOf<MonthCell>()
        repeat(leadingBlanks) {
            cells += MonthCell(dayOfMonth = null, date = null)
        }
        for (day in 1..yearMonth.lengthOfMonth()) {
            val date = firstOfMonth.withDayOfMonth(day)
            cells += MonthCell(dayOfMonth = day, date = date.toString())
        }
        while (cells.size % 7 != 0) {
            cells += MonthCell(dayOfMonth = null, date = null)
        }
        return cells
    }
}
