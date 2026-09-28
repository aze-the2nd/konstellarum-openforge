package de.konstellarum.synesis.notes

import android.content.Context
import de.konstellarum.synesis.core.domain.Note
import de.konstellarum.synesis.core.domain.NoteRepository
import de.konstellarum.synesis.core.store.FileBackedListStore
import de.konstellarum.synesis.core.store.JsonFileStore
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class FileNoteRepository(context: Context) : NoteRepository {

    private val store = FileBackedListStore(
        JsonFileStore.of<List<Note>>(File(context.filesDir, FILE_NAME)),
    )

    override val notes: StateFlow<List<Note>> = store.items

    override fun addNote(title: String, body: String) {
        val now = System.currentTimeMillis()
        val note = Note(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            body = body,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        store.mutate { it + note }
    }

    override fun updateNote(note: Note) {
        val updated = note.copy(updatedAtEpochMillis = System.currentTimeMillis())
        store.mutate { list -> list.map { if (it.id == updated.id) updated else it } }
    }

    override fun removeNote(id: String) {
        store.mutate { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val FILE_NAME = "notes.json"
    }
}
