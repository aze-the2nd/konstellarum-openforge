package de.konstellarum.synesis.core.sensor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartWindowLogicTest {

    // ----- temperatureBounds -----

    @Test
    fun `bounds use half degree steps for small spans`() {
        val bounds = ChartWindowLogic.temperatureBounds(14.2, 14.8)
        assertEquals(0.5, bounds.step)
        assertTrue(bounds.lo <= 14.2)
        assertTrue(bounds.hi >= 14.8)
        assertTrue((bounds.hi - bounds.lo) >= 0.5)
    }

    @Test
    fun `bounds use whole degree steps for medium spans`() {
        val bounds = ChartWindowLogic.temperatureBounds(12.4, 16.1)
        assertEquals(1.0, bounds.step)
        assertEquals(12.0, bounds.lo)
        assertEquals(17.0, bounds.hi)
    }

    @Test
    fun `bounds use two degree steps for wide spans`() {
        val bounds = ChartWindowLogic.temperatureBounds(5.3, 14.9)
        assertEquals(2.0, bounds.step)
        assertTrue(bounds.lo <= 5.3)
        assertTrue(bounds.hi >= 14.9)
    }

    @Test
    fun `bounds handle a constant series`() {
        val bounds = ChartWindowLogic.temperatureBounds(14.0, 14.0)
        assertTrue(bounds.lo < 14.0)
        assertTrue(bounds.hi > 14.0)
        assertTrue(bounds.hi - bounds.lo >= bounds.step)
    }

    // ----- clampWindow -----

    @Test
    fun `window inside extent stays unchanged`() {
        val requested = ChartWindow(startSec = 200, endSec = 800)
        val clamped = ChartWindowLogic.clampWindow(requested, dataMinSec = 0, dataMaxSec = 1000)
        assertEquals(requested, clamped)
    }

    @Test
    fun `window beyond data end is pulled back into the extent`() {
        val requested = ChartWindow(startSec = 700, endSec = 1300)
        val clamped = ChartWindowLogic.clampWindow(requested, dataMinSec = 0, dataMaxSec = 1000)
        assertEquals(1000, clamped.endSec)
        assertEquals(1000 - clamped.spanSec, clamped.startSec)
    }

    @Test
    fun `span smaller than the minimum is widened when the extent allows it`() {
        val requested = ChartWindow(startSec = 400, endSec = 401)
        val clamped = ChartWindowLogic.clampWindow(requested, dataMinSec = 0, dataMaxSec = 1000)
        assertEquals(ChartWindowLogic.MIN_WINDOW_SEC, clamped.spanSec)
    }

    @Test
    fun `span cannot exceed the data extent`() {
        val requested = ChartWindow(startSec = -5000, endSec = 5000)
        val clamped = ChartWindowLogic.clampWindow(requested, dataMinSec = 0, dataMaxSec = 1000)
        assertEquals(1000, clamped.spanSec)
        assertTrue(clamped.startSec >= 0)
        assertTrue(clamped.endSec <= 1000)
    }

    @Test
    fun `empty extent yields the degenerate window`() {
        val clamped = ChartWindowLogic.clampWindow(
            ChartWindow(startSec = 5, endSec = 9),
            dataMinSec = 42,
            dataMaxSec = 42,
        )
        assertEquals(ChartWindow(42, 42), clamped)
    }

    // ----- time ticks -----

    @Test
    fun `ticks are epoch aligned and cover the window`() {
        val start = 100_000L // arbitrary absolute epoch offset
        val end = start + 6 * 60 * 60
        val ticks = ChartWindowLogic.timeTicks(start, end)
        assertTrue(ticks.isNotEmpty())
        ticks.forEach { tick ->
            assertEquals(0L, tick % 900)
            assertTrue(tick >= start)
            assertTrue(tick <= end)
        }
        assertTrue(ticks.size >= 4)
    }

    @Test
    fun `daily intervals for week long spans`() {
        val start = 100_000L
        val end = start + 5L * 24 * 60 * 60
        val ticks = ChartWindowLogic.timeTicks(start, end)
        ticks.forEach { assertEquals(0L, it % (12 * 60 * 60)) }
        assertTrue(ticks.size in 8..11)
    }

    @Test
    fun `no ticks for a degenerate window`() {
        assertEquals(emptyList(), ChartWindowLogic.timeTicks(500, 500))
    }

    @Test
    fun `tick interval grows with the span`() {
        assertTrue(ChartWindowLogic.timeTickIntervalSec(30 * 60) <= ChartWindowLogic.timeTickIntervalSec(3 * 60 * 60))
        assertTrue(ChartWindowLogic.timeTickIntervalSec(3 * 60 * 60) < ChartWindowLogic.timeTickIntervalSec(10L * 24 * 60 * 60))
    }

    // ----- presets -----

    @Test
    fun `preset windows anchor at now and clamp to the data extent`() {
        val now = 1_000_000L
        val window = TimeRangePreset.LAST_24_HOURS.window(now, dataMinSec = now - 3600, dataMaxSec = now)
        assertEquals(now, window.endSec)
        assertEquals(now - 3600, window.startSec)
    }

    @Test
    fun `all preset covers the whole extent`() {
        val now = 1_000_000L
        val window = TimeRangePreset.ALL.window(now, dataMinSec = now - 99_999, dataMaxSec = now - 10)
        assertEquals(now - 99_999, window.startSec)
        assertEquals(now - 10, window.endSec)
    }
}
