package de.konstellarum.synesis.core.store

import kotlinx.serialization.json.Json

/**
 * Shared JSON codec for all Synesis persistence files. Unknown keys are ignored so older
 * files stay readable after model extensions.
 */
object SynesisJson {

    val codec: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
}
