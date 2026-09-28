package de.konstellarum.synesis.core.calendar

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IsoDatesTest {

    @Test
    fun `valid iso date parses`() {
        assertEquals(LocalDate.of(2026, 9, 28), IsoDates.parse("2026-09-28"))
    }

    @Test
    fun `invalid date formats yield null`() {
        assertNull(IsoDates.parse("28.09.2026"))
        assertNull(IsoDates.parse("not-a-date"))
        assertNull(IsoDates.parse(""))
    }

    @Test
    fun `epoch millis round-trips through local date`() {
        val date = LocalDate.of(2026, 9, 28)

        val millis = IsoDates.toEpochMillis(date)

        assertEquals(date, IsoDates.fromEpochMillis(millis))
    }

    @Test
    fun `epoch conversion treats dates as utc calendar days`() {
        val millis = IsoDates.toEpochMillis(LocalDate.of(1970, 1, 1))

        assertEquals(0L, millis)
    }
}
