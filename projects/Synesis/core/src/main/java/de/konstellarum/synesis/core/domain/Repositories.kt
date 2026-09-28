package de.konstellarum.synesis.core.domain

import kotlinx.coroutines.flow.StateFlow

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

interface TranscriptRepository {
    val transcripts: StateFlow<List<Transcript>>

    fun addTranscript(text: String)
    fun removeTranscript(id: String)
}
