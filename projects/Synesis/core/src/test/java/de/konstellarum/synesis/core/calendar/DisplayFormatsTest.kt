package de.konstellarum.synesis.core.calendar

import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayFormatsTest {

    @Test
    fun `month title uses german full month name`() {
        assertEquals("September 2026", DisplayFormats.monthTitle(YearMonth.of(2026, 9)))
    }

    @Test
    fun `weekday labels are german short names starting monday`() {
        assertEquals(listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"), DisplayFormats.weekdayShortLabels)
    }
}
