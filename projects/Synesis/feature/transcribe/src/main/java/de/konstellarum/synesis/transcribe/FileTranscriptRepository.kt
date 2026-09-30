package de.konstellarum.synesis.transcribe

import android.content.Context
import de.konstellarum.synesis.core.domain.Transcript
import de.konstellarum.synesis.core.domain.TranscriptRepository
import de.konstellarum.synesis.core.store.FileBackedListStore
import de.konstellarum.synesis.core.store.JsonFileStore
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

/**
 * Stores each transcript as a package folder under `filesDir/transcripts/<uuid>/`
 * holding the audio recording (`audio.m4a`) and the transcribed text
 * (`transcript.txt`). The index file (`transcripts.json`) keeps the list entries;
 * the folder name is the transcript id, so the entry and the package stay linked.
 */
class FileTranscriptRepository(context: Context) : TranscriptRepository {

    private val rootDir = File(context.filesDir, PACKAGES_DIR)

    private val store = FileBackedListStore(
        JsonFileStore.of<List<Transcript>>(File(context.filesDir, FILE_NAME)),
    )

    override val transcripts: StateFlow<List<Transcript>> = store.items

    override fun newPackageDir(): File {
        val dir = File(rootDir, UUID.randomUUID().toString())
        check(dir.mkdirs()) { "Transkript-Ordner konnte nicht angelegt werden" }
        return dir
    }

    override fun commitPackage(packageDir: File, text: String) {
        val normalized = text.trim()
        File(packageDir, TRANSCRIPT_FILE_NAME).writeText(normalized)
        val transcript = Transcript(
            id = packageDir.name,
            text = normalized,
            createdAtEpochMillis = System.currentTimeMillis(),
            packageDir = packageDir.name,
        )
        store.mutate { list -> listOf(transcript) + list }
    }

    override fun removeTranscript(id: String) {
        val entry = store.items.value.firstOrNull { it.id == id }
        entry?.packageDir?.let { packageName ->
            File(rootDir, packageName).deleteRecursively()
        }
        store.mutate { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val FILE_NAME = "transcripts.json"
        const val PACKAGES_DIR = "transcripts"
        const val AUDIO_FILE_NAME = "audio.m4a"
        const val TRANSCRIPT_FILE_NAME = "transcript.txt"
    }
}
