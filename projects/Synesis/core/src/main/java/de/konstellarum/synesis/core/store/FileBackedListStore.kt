package de.konstellarum.synesis.core.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * File-backed list store with a StateFlow surface. Mutations are applied to the current
 * list, emitted, and persisted synchronously; readers of the flow see the persisted state.
 */
class FileBackedListStore<T : Any>(
    private val fileStore: JsonFileStore<List<T>>,
    initial: List<T> = emptyList(),
) {

    private val _items = MutableStateFlow(fileStore.load() ?: initial)
    val items: StateFlow<List<T>> = _items.asStateFlow()

    fun mutate(transform: (List<T>) -> List<T>) {
        val updated = transform(_items.value)
        _items.value = updated
        fileStore.save(updated)
    }
}
