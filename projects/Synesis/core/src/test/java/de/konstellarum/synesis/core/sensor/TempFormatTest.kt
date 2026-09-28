package de.konstellarum.synesis.core.sensor

import kotlin.test.Test
import kotlin.test.assertEquals

class TempFormatTest {

    @Test
    fun `formats with german decimal separator and one decimal`() {
        assertEquals("12,3 °C", TempFormat.celsius(12.34))
    }

    @Test
    fun `whole degrees keep the decimal place`() {
        assertEquals("12,0 °C", TempFormat.celsius(12.0))
    }

    @Test
    fun `negative values format correctly`() {
        assertEquals("-2,5 °C", TempFormat.celsius(-2.46))
    }

    @Test
    fun `rounding is half-up at the first decimal`() {
        assertEquals("12,4 °C", TempFormat.celsius(12.35))
    }
}
