package de.konstellarum.synesis.core.sensor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartScalingTest {

    private fun sample(t: Long, tempC: Double): TempSample = TempSample(t = t, tempC = tempC)

    @Test
    fun `min maps to bottom and max to top within padding`() {
        val samples = listOf(sample(1L, 10.0), sample(2L, 20.0))

        val points = ChartScaling.fit(samples, width = 100f, height = 100f, padding = 10f)

        assertEquals(10f, points[0].x)
        assertEquals(90f, points[0].y)
        assertEquals(90f, points[1].x)
        assertEquals(10f, points[1].y)
    }

    @Test
    fun `constant series maps to the vertical middle`() {
        val samples = listOf(sample(1L, 12.0), sample(2L, 12.0), sample(3L, 12.0))

        val points = ChartScaling.fit(samples, width = 100f, height = 100f, padding = 10f)

        assertTrue(points.all { it.y == 50f })
    }

    @Test
    fun `empty input yields empty points`() {
        assertTrue(ChartScaling.fit(emptyList(), 100f, 100f, 10f).isEmpty())
    }

    @Test
    fun `single sample yields one centered point`() {
        val points = ChartScaling.fit(listOf(sample(1L, 12.0)), 100f, 100f, 10f)

        assertEquals(1, points.size)
        assertEquals(50f, points[0].x)
        assertEquals(50f, points[0].y)
    }

    @Test
    fun `many samples are downsampled to maxPoints preserving first and last`() {
        val samples = (1L..500L).map { sample(it, 10.0 + (it % 10)) }

        val points = ChartScaling.fit(samples, 100f, 100f, 10f, maxPoints = 120)

        assertEquals(120, points.size)
        assertEquals(10f, points.first().x)
        assertEquals(90f, points.last().x)
    }

    @Test
    fun `downsampling keeps values monotonic in time across x axis`() {
        val samples = (1L..100L).map { sample(it, 10.0 + it) }

        val points = ChartScaling.fit(samples, 100f, 100f, 10f, maxPoints = 25)

        assertEquals(points.map { it.x }.sorted(), points.map { it.x })
    }
}
