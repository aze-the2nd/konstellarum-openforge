package de.konstellarum.synesis.core.sensor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SensorResponseParserTest {

    @Test
    fun `valid response parses samples in ascending time order`() {
        val json = """
            {"ok":true,"count":3,"values":[
              {"t":1700000003,"temp_c":12.3},
              {"t":1700000001,"temp_c":11.9},
              {"t":1700000002,"temp_c":12.1}
            ]}
        """.trimIndent()

        val result = SensorResponseParser.parse(json)

        assertTrue(result is SensorResponseParser.ParseResult.Success)
        val samples = (result as SensorResponseParser.ParseResult.Success).samples
        assertEquals(listOf(1700000001L, 1700000002L, 1700000003L), samples.map { it.t })
        assertEquals(11.9, samples[0].tempC)
        assertEquals(12.3, samples[2].tempC)
    }

    @Test
    fun `response with ok false yields failure with the error message`() {
        val json = """{"ok":false,"error":"no data"}"""

        val result = SensorResponseParser.parse(json)

        assertTrue(result is SensorResponseParser.ParseResult.Failure)
        assertEquals("no data", (result as SensorResponseParser.ParseResult.Failure).message)
    }

    @Test
    fun `malformed json yields failure`() {
        val result = SensorResponseParser.parse("this is not json")

        assertTrue(result is SensorResponseParser.ParseResult.Failure)
    }

    @Test
    fun `empty values array parses to empty success`() {
        val json = """{"ok":true,"count":0,"values":[]}"""

        val result = SensorResponseParser.parse(json)

        assertTrue(result is SensorResponseParser.ParseResult.Success)
        assertTrue((result as SensorResponseParser.ParseResult.Success).samples.isEmpty())
    }
}
