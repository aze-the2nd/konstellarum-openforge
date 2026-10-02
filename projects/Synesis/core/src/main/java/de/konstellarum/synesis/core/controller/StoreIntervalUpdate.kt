package de.konstellarum.synesis.core.controller

/**
 * Outcome of a store-interval write. The device ignores invalid writes (wrong
 * length or out of range), so the app always verifies by reading the value
 * back after the write.
 */
sealed interface StoreIntervalUpdate {

    /** The new interval is active and persisted on the device. */
    data class Applied(val seconds: Int) : StoreIntervalUpdate

    /** The write went out but the device kept its previous value. */
    data class Rejected(val written: Int, val current: Int) : StoreIntervalUpdate

    /** The write could not be verified or the characteristic is unavailable. */
    data class Unavailable(val message: String) : StoreIntervalUpdate
}

/**
 * Classifies a write plus its read-back into a [StoreIntervalUpdate].
 * [readBack] is null when the read failed or the payload was malformed.
 */
object StoreIntervalUpdateResolver {

    fun resolve(written: Int, readBack: Int?): StoreIntervalUpdate = when {
        readBack == null -> StoreIntervalUpdate.Unavailable(
            "Wert konnte nicht verifiziert werden (Rücklesen fehlgeschlagen).",
        )

        readBack != written -> StoreIntervalUpdate.Rejected(written, readBack)

        else -> StoreIntervalUpdate.Applied(readBack)
    }
}
