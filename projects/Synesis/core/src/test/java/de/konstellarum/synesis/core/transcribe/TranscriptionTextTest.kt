package de.konstellarum.synesis.core.transcribe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TranscriptionTextTest {

    @Test
    fun `normalize trims text and collapses whitespace`() {
        assertEquals(
            "Synesis lernt aus Sprache",
            TranscriptionText.normalize("  Synesis\n\tlernt   aus Sprache  "),
        )
    }

    @Test
    fun `firstUsable returns first non blank normalized candidate`() {
        val selected = TranscriptionText.firstUsable(listOf("   ", "  erster\nTreffer  ", "zweiter"))

        assertEquals("erster Treffer", selected)
    }

    @Test
    fun `firstUsable returns null when all candidates are blank`() {
        assertNull(TranscriptionText.firstUsable(listOf("", "   ", "\n")))
    }
}
