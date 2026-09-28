package de.konstellarum.synesis.core.store

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.io.File

/**
 * Version-safe JSON file persistence for a single value.
 *
 * Guarantees:
 * - missing file loads as null (caller decides the default),
 * - corrupt content is moved to `<name>.corrupt` instead of being silently overwritten,
 * - saves write to a temp file first and then replace the target.
 */
class JsonFileStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
) {

    fun load(): T? {
        if (!file.exists()) return null
        return try {
            SynesisJson.codec.decodeFromString(serializer, file.readText())
        } catch (_: Throwable) {
            quarantineCorruptFile()
            null
        }
    }

    fun save(value: T) {
        file.parentFile?.mkdirs()
        val content = SynesisJson.codec.encodeToString(serializer, value)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(content)
        if (!temp.renameTo(file)) {
            file.writeText(content)
            temp.delete()
        }
    }

    private fun quarantineCorruptFile() {
        val quarantine = File(file.parentFile, "${file.name}.corrupt")
        file.copyTo(quarantine, overwrite = true)
        file.delete()
    }

    companion object {
        inline fun <reified T> of(file: File): JsonFileStore<T> =
            JsonFileStore(file, serializer())
    }
}
