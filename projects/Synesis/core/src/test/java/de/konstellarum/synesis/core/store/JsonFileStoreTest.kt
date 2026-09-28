package de.konstellarum.synesis.core.store

import kotlinx.serialization.Serializable
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Serializable
data class TestEntry(val id: String, val label: String)

class JsonFileStoreTest {

    private fun tempFile(name: String): File =
        File.createTempFile(name, ".json").also { it.delete() }

    @Test
    fun `missing file loads null`() {
        val store = JsonFileStore.of<List<TestEntry>>(tempFile("missing"))

        assertNull(store.load())
    }

    @Test
    fun `save then load round-trips entries`() {
        val file = tempFile("roundtrip")
        val store = JsonFileStore.of<List<TestEntry>>(file)
        val entries = listOf(TestEntry("1", "one"), TestEntry("2", "two"))

        store.save(entries)

        assertEquals(entries, store.load())
    }

    @Test
    fun `empty list round-trips`() {
        val file = tempFile("empty")
        val store = JsonFileStore.of<List<TestEntry>>(file)

        store.save(emptyList())

        assertEquals(emptyList(), store.load())
    }

    @Test
    fun `corrupt content is quarantined and loads null`() {
        val file = tempFile("corrupt")
        file.writeText("{ not json at all")

        val store = JsonFileStore.of<List<TestEntry>>(file)

        assertNull(store.load())
        assertFalse(file.exists())
        val quarantine = File(file.parentFile, file.name + ".corrupt")
        assertTrue(quarantine.exists())
        assertEquals("{ not json at all", quarantine.readText())
    }
}
