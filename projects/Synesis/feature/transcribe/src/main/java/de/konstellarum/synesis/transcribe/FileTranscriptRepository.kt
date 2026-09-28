package de.konstellarum.synesis.transcribe

import android.content.Context
import de.konstellarum.synesis.core.domain.Transcript
import de.konstellarum.synesis.core.domain.TranscriptRepository
import de.konstellarum.synesis.core.store.FileBackedListStore
import de.konstellarum.synesis.core.store.JsonFileStore
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class FileTranscriptRepository(context: Context) : TranscriptRepository {

    private val store = FileBackedListStore(
        JsonFileStore.of<List<Transcript>>(File(context.filesDir, FILE_NAME)),
    )

    override val transcripts: StateFlow<List<Transcript>> = store.items

    override fun addTranscript(text: String) {
        val transcript = Transcript(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        store.mutate { list -> listOf(transcript) + list }
    }

    override fun removeTranscript(id: String) {
        store.mutate { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val FILE_NAME = "transcripts.json"
    }
}
