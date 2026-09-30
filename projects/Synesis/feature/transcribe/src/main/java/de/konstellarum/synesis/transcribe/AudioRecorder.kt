package de.konstellarum.synesis.transcribe

import android.media.MediaRecorder
import java.io.File

/**
 * Thin wrapper around [MediaRecorder] producing an AAC recording in an MP4 container
 * (`audio.m4a`) — the format the Whisper endpoint expects (uploaded as `audio/mp4`).
 * The recording stays on-device until it is handed to the private bridge for
 * transcription; nothing is streamed while recording.
 */
class AudioRecorder {

    private var recorder: MediaRecorder? = null

    val isRecording: Boolean
        get() = recorder != null

    fun start(output: File) {
        check(recorder == null) { "Aufnahme läuft bereits" }
        output.parentFile?.mkdirs()
        val mediaRecorder = MediaRecorder()
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(AUDIO_SAMPLING_RATE)
            mediaRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE)
            mediaRecorder.setOutputFile(output.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
        } catch (error: Exception) {
            mediaRecorder.release()
            recorder = null
            output.delete()
            throw error
        }
    }

    fun stop() {
        val mediaRecorder = recorder ?: return
        recorder = null
        try {
            mediaRecorder.stop()
        } catch (_: RuntimeException) {
            // stop() may throw if the recording was too short; release anyway.
        }
        mediaRecorder.release()
    }

    companion object {
        const val AUDIO_SAMPLING_RATE = 44_100
        const val AUDIO_BIT_RATE = 96_000
    }
}
