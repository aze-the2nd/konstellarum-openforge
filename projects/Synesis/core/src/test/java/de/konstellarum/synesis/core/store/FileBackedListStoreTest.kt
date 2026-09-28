package de.konstellarum.synesis.core.store

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FileBackedListStoreTest {

    private fun tempFile(name: String): File =
        File.createTempFile(name, ".json").also { it.delete() }

    @Test
    fun `starts with persisted items when the file exists`() {
        val file = tempFile("persisted")
        JsonFileStore.of<List<TestEntry>>(file).save(listOf(TestEntry("1", "one")))

        val store = FileBackedListStore(JsonFileStore.of<List<TestEntry>>(file))

        assertEquals(listOf(TestEntry("1", "one")), store.items.value)
    }

    @Test
    fun `starts empty when the file is missing`() {
        val store = FileBackedListStore(JsonFileStore.of<List<TestEntry>>(tempFile("missing")))

        assertEquals(emptyList(), store.items.value)
    }

    @Test
    fun `mutate updates the flow and persists to disk`() {
        val file = tempFile("mutate")
        val store = FileBackedListStore(JsonFileStore.of<List<TestEntry>>(file))

        store.mutate { it + TestEntry("1", "one") }

        assertEquals(listOf(TestEntry("1", "one")), store.items.value)
        assertEquals(listOf(TestEntry("1", "one")), JsonFileStore.of<List<TestEntry>>(file).load())
    }

    @Test
    fun `mutate can remove items`() {
        val file = tempFile("remove")
        JsonFileStore.of<List<TestEntry>>(file).save(listOf(TestEntry("1", "one"), TestEntry("2", "two")))
        val store = FileBackedListStore(JsonFileStore.of<List<TestEntry>>(file))

        store.mutate { it.filterNot { entry -> entry.id == "1" } }

        assertEquals(listOf(TestEntry("2", "two")), store.items.value)
        assertEquals(listOf(TestEntry("2", "two")), JsonFileStore.of<List<TestEntry>>(file).load())
    }
}
