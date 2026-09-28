package de.konstellarum.synesis.todos

import android.content.Context
import de.konstellarum.synesis.core.domain.TodoItem
import de.konstellarum.synesis.core.domain.TodoRepository
import de.konstellarum.synesis.core.store.FileBackedListStore
import de.konstellarum.synesis.core.store.JsonFileStore
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

class FileTodoRepository(context: Context) : TodoRepository {

    private val store = FileBackedListStore(
        JsonFileStore.of<List<TodoItem>>(File(context.filesDir, FILE_NAME)),
    )

    override val todos: StateFlow<List<TodoItem>> = store.items

    override fun addTodo(title: String, linkedDate: String?) {
        val todo = TodoItem(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            done = false,
            linkedDate = linkedDate,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        store.mutate { it + todo }
    }

    override fun setTodoDone(id: String, done: Boolean) {
        store.mutate { list -> list.map { if (it.id == id) it.copy(done = done) else it } }
    }

    override fun setTodoLinkedDate(id: String, linkedDate: String?) {
        store.mutate { list -> list.map { if (it.id == id) it.copy(linkedDate = linkedDate) else it } }
    }

    override fun removeTodo(id: String) {
        store.mutate { list -> list.filterNot { it.id == id } }
    }

    companion object {
        const val FILE_NAME = "todos.json"
    }
}
