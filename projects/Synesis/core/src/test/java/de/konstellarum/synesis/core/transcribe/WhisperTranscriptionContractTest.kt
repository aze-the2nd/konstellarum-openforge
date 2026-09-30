package de.konstellarum.synesis.core.transcribe

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhisperTranscriptionContractTest {

    @Test
    fun `parses a successful response and normalizes whitespace`() {
        val body = """{"ok":true,"text":"  Keller-Sensor   heute prüfen. ","model":"whisper-small","language":"de"}"""
        val result = WhisperTranscriptionContract.parseResponse(body)
        assertTrue(result is WhisperTranscriptionResult.Success)
        result as WhisperTranscriptionResult.Success
        assertEquals("Keller-Sensor heute prüfen.", result.text)
        assertEquals("whisper-small", result.model)
    }

    @Test
    fun `failure response carries the server error message`() {
        val body = """{"ok":false,"error":"small model unavailable"}"""
        val result = WhisperTranscriptionContract.parseResponse(body)
        assertTrue(result is WhisperTranscriptionResult.Failure)
        assertEquals("small model unavailable", (result as WhisperTranscriptionResult.Failure).message)
    }

    @Test
    fun `ok response without text is a failure`() {
        val result = WhisperTranscriptionContract.parseResponse("""{"ok":true}""")
        assertTrue(result is WhisperTranscriptionResult.Failure)
    }

    @Test
    fun `blank text is treated as no result`() {
        val result = WhisperTranscriptionContract.parseResponse("""{"ok":true,"text":"   "}""")
        assertTrue(result is WhisperTranscriptionResult.Failure)
    }

    @Test
    fun `malformed json is a failure with a stable message`() {
        val result = WhisperTranscriptionContract.parseResponse("nicht json")
        assertTrue(result is WhisperTranscriptionResult.Failure)
        assertTrue((result as WhisperTranscriptionResult.Failure).message.contains("Ungültige"))
    }

    @Test
    fun `missing ok flag is treated as failure`() {
        val result = WhisperTranscriptionContract.parseResponse("""{"text":"irgendwas"}""")
        assertTrue(result is WhisperTranscriptionResult.Failure)
    }
}
