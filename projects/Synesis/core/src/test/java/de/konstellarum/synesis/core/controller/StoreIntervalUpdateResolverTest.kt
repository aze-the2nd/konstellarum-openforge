package de.konstellarum.synesis.core.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StoreIntervalUpdateResolverTest {

    @Test
    fun `reports applied when the read-back equals the written value`() {
        assertEquals(
            StoreIntervalUpdate.Applied(10),
            StoreIntervalUpdateResolver.resolve(written = 10, readBack = 10),
        )
    }

    @Test
    fun `reports rejected when the device ignored the write`() {
        assertEquals(
            StoreIntervalUpdate.Rejected(written = 3600, current = 60),
            StoreIntervalUpdateResolver.resolve(written = 3600, readBack = 60),
        )
    }

    @Test
    fun `reports rejected even for boundary values outside the allowed range`() {
        // The app validates before writing, but the device contract says an
        // invalid write is silently ignored — the read-back proves it.
        assertEquals(
            StoreIntervalUpdate.Rejected(written = 4, current = 60),
            StoreIntervalUpdateResolver.resolve(written = 4, readBack = 60),
        )
    }

    @Test
    fun `reports unavailable when the read-back could not be obtained`() {
        assertIs<StoreIntervalUpdate.Unavailable>(
            StoreIntervalUpdateResolver.resolve(written = 10, readBack = null),
        )
    }
}
