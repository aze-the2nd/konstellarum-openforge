package de.konstellarum.synesis.core.sensor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TempStatsTest {

    private fun sample(t: Long, tempC: Double): TempSample = TempSample(t = t, tempC = tempC)

    @Test
    fun `stats compute min avg and max over samples`() {
        val samples = listOf(sample(1L, 10.0), sample(2L, 12.0), sample(3L, 14.0))

        val stats = TempStats.compute(samples)

        assertEquals(10.0, stats?.min)
        assertEquals(12.0, stats?.avg)
        assertEquals(14.0, stats?.max)
    }

    @Test
    fun `single sample yields min avg max all equal`() {
        val stats = TempStats.compute(listOf(sample(1L, 11.5)))

        assertEquals(11.5, stats?.min)
        assertEquals(11.5, stats?.avg)
        assertEquals(11.5, stats?.max)
    }

    @Test
    fun `empty input yields null stats`() {
        assertNull(TempStats.compute(emptyList()))
    }

    @Test
    fun `avg is the arithmetic mean within floating tolerance`() {
        val samples = listOf(sample(1L, 10.1), sample(2L, 10.2), sample(3L, 10.3))

        val stats = TempStats.compute(samples)

        assertTrue(kotlin.math.abs(stats!!.avg - 10.2) < 1e-9)
    }
}
