package de.konstellarum.synesis.core.domain

import de.konstellarum.synesis.core.transcribe.WhisperTranscriptionResult
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Repository contracts live in core so modules can consume each other's data through the
 * shell-owned instances without depending on concrete implementations.
 */
interface NoteRepository {
    val notes: StateFlow<List<Note>>

    fun addNote(title: String, body: String = "")
    fun updateNote(note: Note)
    fun removeNote(id: String)
}

interface TodoRepository {
    val todos: StateFlow<List<TodoItem>>

    fun addTodo(title: String, linkedDate: String? = null)
    fun setTodoDone(id: String, done: Boolean)
    fun setTodoLinkedDate(id: String, linkedDate: String?)
    fun removeTodo(id: String)
}

interface EventRepository {
    val events: StateFlow<List<CalendarEvent>>

    fun addEvent(title: String, date: String)
    fun removeEvent(id: String)
}

/**
 * Transcripts are stored as packages: a folder that holds the audio recording
 * (`audio.m4a`) and the transcribed text (`transcript.txt`) side by side.
 */
interface TranscriptRepository {
    val transcripts: StateFlow<List<Transcript>>

    /** Creates a fresh, empty package folder; its name becomes the transcript id. */
    fun newPackageDir(): File

    /** Writes the transcript text file into the package and adds the index entry. */
    fun commitPackage(packageDir: File, text: String)

    /** Removes the index entry and, if present, deletes the package folder. */
    fun removeTranscript(id: String)
}

/**
 * Transcribes an audio recording via the private Whisper endpoint on the bridge.
 */
interface WhisperTranscriptionRepository {
    suspend fun transcribe(audioFile: File): WhisperTranscriptionResult
}
