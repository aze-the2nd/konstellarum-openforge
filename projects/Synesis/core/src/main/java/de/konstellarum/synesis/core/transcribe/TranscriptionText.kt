package de.konstellarum.synesis.core.transcribe

/**
 * Small pure helper for turning speech-recognizer candidates into a stable transcript string.
 */
object TranscriptionText {

    private val whitespace = Regex("\\s+")

    fun normalize(input: String): String = input.trim().replace(whitespace, " ")

    fun firstUsable(candidates: List<String>): String? =
        candidates.asSequence()
            .map(::normalize)
            .firstOrNull { it.isNotBlank() }
}
