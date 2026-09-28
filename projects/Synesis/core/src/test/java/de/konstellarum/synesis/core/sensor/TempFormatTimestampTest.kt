package de.konstellarum.synesis.core.sensor

import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class TempFormatTimestampTest {

    @Test
    fun `formats unix seconds as german date and time`() {
        // 1700000000 = 2023-11-14T22:13:20Z = 23:13 in Europe/Berlin
        assertEquals("14.11. 23:13", TempFormat.timestamp(1_700_000_000L, ZoneId.of("Europe/Berlin")))
    }

    @Test
    fun `epoch zero formats as 1970`() {
        assertEquals("01.01. 01:00", TempFormat.timestamp(0L, ZoneId.of("Europe/Berlin")))
    }
}
