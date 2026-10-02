package de.konstellarum.synesis.core.sensor

import kotlin.test.Test
import kotlin.test.assertEquals

class TempHistoryMergeTest {

    private fun sample(t: Long, tempC: Double): TempSample = TempSample(t = t, tempC = tempC)

    @Test
    fun `merged history is in ascending time order with the newest sample last`() {
        val fetched = listOf(sample(3L, 12.3), sample(1L, 11.9), sample(2L, 12.1))
        val cached = listOf(sample(4L, 12.4), sample(0L, 11.8))

        val merged = TempHistory.merge(fetched, cached, maxHistory = 10)

        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), merged.map { it.t })
        assertEquals(12.4, merged.last().tempC)
    }

    @Test
    fun `duplicates by timestamp are removed and the fetched sample wins`() {
        val fetched = listOf(sample(1L, 11.9), sample(2L, 12.0))
        val cached = listOf(sample(2L, 12.05), sample(3L, 12.1))

        val merged = TempHistory.merge(fetched, cached, maxHistory = 10)

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.t })
        assertEquals(12.0, merged[1].tempC)
    }

    @Test
    fun `history is capped keeping the newest samples`() {
        val fetched = listOf(sample(2L, 12.0), sample(1L, 11.9))
        val cached = listOf(sample(3L, 12.1))

        val merged = TempHistory.merge(fetched, cached, maxHistory = 2)

        assertEquals(listOf(2L, 3L), merged.map { it.t })
    }

    @Test
    fun `merge with an empty cache keeps the fetched samples`() {
        val fetched = listOf(sample(2L, 12.0), sample(1L, 11.9))

        val merged = TempHistory.merge(fetched, emptyList(), maxHistory = 10)

        assertEquals(listOf(1L, 2L), merged.map { it.t })
    }

    @Test
    fun `merge with empty fetched samples normalizes the cached order`() {
        val cached = listOf(sample(3L, 12.1), sample(2L, 12.0), sample(1L, 11.9))

        val merged = TempHistory.merge(emptyList(), cached, maxHistory = 10)

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.t })
    }
}
