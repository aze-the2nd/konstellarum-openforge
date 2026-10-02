package de.konstellarum.synesis.core.controller

/**
 * The controller's store interval: how often the sensor controller writes a
 * measurement into its database. Configured over BLE via the `StoreInterval`
 * characteristic — uint16 little-endian seconds, allowed range 5..3600,
 * persisted in NVS on the device (firmware v3, 2026-10-02).
 *
 * Independent from the on-device chart ring buffer, which keeps a fixed 60 s
 * cadence regardless of this value.
 */
object StoreIntervalConfig {

    const val MIN_SECONDS = 5
    const val MAX_SECONDS = 3600
    const val BYTE_LENGTH = 2

    fun isValid(seconds: Int): Boolean = seconds in MIN_SECONDS..MAX_SECONDS

    /** Encodes [seconds] as uint16 little-endian (the wire format). */
    fun encode(seconds: Int): ByteArray = byteArrayOf(
        (seconds and 0xFF).toByte(),
        ((seconds shr 8) and 0xFF).toByte(),
    )

    /** Decodes a uint16 little-endian payload; null when the size is not 2. */
    fun decode(payload: ByteArray): Int? {
        if (payload.size != BYTE_LENGTH) return null
        val low = payload[0].toInt() and 0xFF
        val high = payload[1].toInt() and 0xFF
        return (high shl 8) or low
    }
}
