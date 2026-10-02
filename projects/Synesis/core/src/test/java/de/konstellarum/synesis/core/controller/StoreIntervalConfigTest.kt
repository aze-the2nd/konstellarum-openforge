package de.konstellarum.synesis.core.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoreIntervalConfigTest {

    @Test
    fun `accepts values inside the 5 to 3600 range`() {
        assertTrue(StoreIntervalConfig.isValid(5))
        assertTrue(StoreIntervalConfig.isValid(10))
        assertTrue(StoreIntervalConfig.isValid(60))
        assertTrue(StoreIntervalConfig.isValid(3600))
    }

    @Test
    fun `rejects values outside the 5 to 3600 range`() {
        assertFalse(StoreIntervalConfig.isValid(4))
        assertFalse(StoreIntervalConfig.isValid(0))
        assertFalse(StoreIntervalConfig.isValid(-10))
        assertFalse(StoreIntervalConfig.isValid(3601))
    }

    @Test
    fun `encodes seconds as uint16 little-endian`() {
        assertEquals(listOf<Byte>(0x0A, 0x00), StoreIntervalConfig.encode(10).toList())
        assertEquals(listOf<Byte>(0x3C, 0x00), StoreIntervalConfig.encode(60).toList())
        assertEquals(listOf<Byte>(0x2C, 0x01), StoreIntervalConfig.encode(300).toList())
        assertEquals(listOf<Byte>(0x00, 0x01), StoreIntervalConfig.encode(256).toList())
        assertEquals(listOf<Byte>(0x10, 0x0E), StoreIntervalConfig.encode(3600).toList())
    }

    @Test
    fun `decodes uint16 little-endian payloads`() {
        assertEquals(10, StoreIntervalConfig.decode(byteArrayOf(0x0A, 0x00)))
        assertEquals(60, StoreIntervalConfig.decode(byteArrayOf(0x3C, 0x00)))
        assertEquals(300, StoreIntervalConfig.decode(byteArrayOf(0x2C, 0x01)))
        assertEquals(3600, StoreIntervalConfig.decode(byteArrayOf(0x10, 0x0E)))
    }

    @Test
    fun `round-trips boundary values`() {
        for (seconds in listOf(5, 59, 60, 61, 3599, 3600)) {
            assertEquals(seconds, StoreIntervalConfig.decode(StoreIntervalConfig.encode(seconds)))
        }
    }

    @Test
    fun `rejects payloads that are not exactly two bytes`() {
        assertNull(StoreIntervalConfig.decode(byteArrayOf()))
        assertNull(StoreIntervalConfig.decode(byteArrayOf(0x0A)))
        assertNull(StoreIntervalConfig.decode(byteArrayOf(0x0A, 0x00, 0x00)))
    }
}
