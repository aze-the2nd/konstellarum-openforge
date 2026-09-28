package de.konstellarum.synesis.calendar

import android.content.Context
import de.konstellarum.synesis.core.domain.CalendarEvent
import de.konstellarum.synesis.core.domain.EventRepository
import de.konstellarum.synesis.core.store.FileBackedListStore
import de.konstellarum.synesis.core.store.JsonFileStore
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class FileEventRepository(context: Context) : EventRepository {

    private val store = FileBackedListStore(
        JsonFileStore.of<List<CalendarEvent>>(File(context.filesDir, FILE_NAME)),
    )

    override val events: StateFlow<List<CalendarEvent>> = store.items

    override fun addEvent(title: String, date: String) {
        val event = CalendarEvent(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            date = date,
        )
        store.mutate { it + event }
    }

    override fun removeEvent(id: String) {
        store.mutate { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val FILE_NAME = "events.json"
    }
}
