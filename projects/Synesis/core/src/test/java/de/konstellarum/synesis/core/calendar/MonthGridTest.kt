package de.konstellarum.synesis.core.calendar

import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthGridTest {

    @Test
    fun `february 2026 starts on a sunday so monday-first grid has six leading blanks`() {
        val cells = MonthGrid.cellsFor(YearMonth.of(2026, 2))

        assertEquals(6, cells.takeWhile { it.dayOfMonth == null }.size)
        assertEquals(1, cells.first { it.dayOfMonth != null }.dayOfMonth)
    }

    @Test
    fun `may 2026 starts on a friday so monday-first grid has four leading blanks`() {
        val cells = MonthGrid.cellsFor(YearMonth.of(2026, 5))

        assertEquals(4, cells.takeWhile { it.dayOfMonth == null }.size)
        assertEquals(1, cells.first { it.dayOfMonth != null }.dayOfMonth)
    }

    @Test
    fun `february 2026 has 28 days and fills exactly five weeks`() {
        val cells = MonthGrid.cellsFor(YearMonth.of(2026, 2))

        val days = cells.mapNotNull { it.dayOfMonth }
        assertEquals(28, days.size)
        assertEquals((1..28).toList(), days)
        assertEquals(35, cells.size)
    }

    @Test
    fun `leap february 2024 has 29 days`() {
        val cells = MonthGrid.cellsFor(YearMonth.of(2024, 2))

        assertEquals(29, cells.mapNotNull { it.dayOfMonth }.size)
        assertEquals(29, cells.first { it.dayOfMonth == 29 }.dayOfMonth)
    }

    @Test
    fun `every month grid has a multiple of seven cells`() {
        for (month in 1..12) {
            val size = MonthGrid.cellsFor(YearMonth.of(2026, month)).size
            assertTrue(size % 7 == 0, "month $month produced $size cells")
        }
    }

    @Test
    fun `day cells carry correct iso dates`() {
        val cells = MonthGrid.cellsFor(YearMonth.of(2026, 5))

        assertEquals("2026-05-01", cells.first { it.dayOfMonth == 1 }.date)
        assertEquals("2026-05-31", cells.first { it.dayOfMonth == 31 }.date)
    }

    @Test
    fun `trailing cells after the last day are blank`() {
        val cells = MonthGrid.cellsFor(YearMonth.of(2026, 2))
        val lastDayIndex = cells.indexOfLast { it.dayOfMonth != null }

        assertTrue(cells.drop(lastDayIndex + 1).all { it.dayOfMonth == null && it.date == null })
    }
}
