package de.konstellarum.synesis.core.transcribe

import de.konstellarum.synesis.core.store.SynesisJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/**
 * Wire contract for the private Whisper transcription endpoint on the iot-db-bridge:
 * the app uploads the raw audio recording (AAC in an MP4 container, `audio/mp4`) and
 * receives the transcribed text. The app embeds no model or API credentials; the
 * concrete Whisper model stays server-side.
 */
@Serializable
data class WhisperTranscriptionResponse(
    val ok: Boolean = false,
    val text: String? = null,
    val model: String? = null,
    val language: String? = null,
    val error: String? = null,
)

sealed interface WhisperTranscriptionResult {
    data class Success(
        val text: String,
        val model: String?,
    ) : WhisperTranscriptionResult

    data class Failure(val message: String) : WhisperTranscriptionResult
}

object WhisperTranscriptionContract {

    const val ENDPOINT_PATH = "/transcripts/whisper"

    /** Raw-bytes upload: the recording is sent as the request body. */
    const val CONTENT_TYPE = "audio/mp4"

    const val MAX_AUDIO_BYTES = 25L * 1024 * 1024

    fun parseResponse(body: String): WhisperTranscriptionResult {
        return try {
            val response = SynesisJson.codec.decodeFromString(
                WhisperTranscriptionResponse.serializer(),
                body,
            )
            if (!response.ok) {
                WhisperTranscriptionResult.Failure(
                    response.error?.takeIf { it.isNotBlank() } ?: "Whisper-Transkription fehlgeschlagen",
                )
            } else {
                val text = response.text.orEmpty().trim().replace(Regex("\\s+"), " ")
                if (text.isBlank()) {
                    WhisperTranscriptionResult.Failure("Whisper-Transkription ohne Ergebnis")
                } else {
                    WhisperTranscriptionResult.Success(text = text, model = response.model)
                }
            }
        } catch (_: SerializationException) {
            WhisperTranscriptionResult.Failure("Ungültige Whisper-Antwort")
        } catch (_: IllegalArgumentException) {
            WhisperTranscriptionResult.Failure("Ungültige Whisper-Antwort")
        }
    }
}
