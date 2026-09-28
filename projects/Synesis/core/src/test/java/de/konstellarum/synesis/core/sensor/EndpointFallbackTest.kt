package de.konstellarum.synesis.core.sensor

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EndpointFallbackTest {

    @Test
    fun `returns the first successful candidate`() = runBlocking {
        val attempts = mutableListOf<String>()
        val result = EndpointFallback.firstSuccess(listOf("a", "b", "c")) { candidate ->
            attempts += candidate
            if (candidate == "b") "hit" else null
        }
        assertEquals("hit", result)
        assertEquals(listOf("a", "b"), attempts)
    }

    @Test
    fun `skips failing candidates in order`() = runBlocking {
        val attempts = mutableListOf<String>()
        val result = EndpointFallback.firstSuccess(listOf("a", "b")) { candidate ->
            attempts += candidate
            null
        }
        assertNull(result)
        assertEquals(listOf("a", "b"), attempts)
    }

    @Test
    fun `returns null for an empty candidate list`() = runBlocking {
        assertNull(EndpointFallback.firstSuccess(emptyList()) { "never" })
    }

    @Test
    fun `a success on the first candidate short-circuits`() = runBlocking {
        var calls = 0
        val result = EndpointFallback.firstSuccess(listOf("a", "b")) {
            calls += 1
            "hit"
        }
        assertEquals("hit", result)
        assertEquals(1, calls)
    }
}
