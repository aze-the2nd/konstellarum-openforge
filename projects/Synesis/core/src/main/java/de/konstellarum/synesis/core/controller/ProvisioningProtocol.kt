package de.konstellarum.synesis.core.controller

/**
 * Wire contract between the Synesis app (BLE central, Android) and the sensor
 * controller `iot-rtd-sensor` (BLE peripheral, NimBLE-Arduino GATT server).
 *
 * Agreed 2026-09-28 (Alex' Auftrag #888) with Aurora, who implements the
 * firmware side against these exact UUIDs and formats. Canonical copy:
 * `docs/BLE_PROVISIONING_CONTRACT.md`.
 */
object ProvisioningProtocol {

    /** Advertising name of the controller peripheral. */
    const val DEVICE_ADVERTISING_NAME = "iot-rtd-sensor"

    const val SERVICE_UUID = "bbf1ab36-e36e-4b35-a238-e33e32684d8f"
    const val CHAR_SSID_UUID = "e2ee8fe3-eda6-4b0c-8795-5d127191a83b"
    const val CHAR_PASSWORD_UUID = "64f853c5-3007-4986-8f0d-1442969bdf42"
    const val CHAR_APPLY_UUID = "15b55d7c-c1ec-4e21-ab83-56da84ca9dbf"
    const val CHAR_STATUS_UUID = "ef3f6a85-2f63-490f-9803-bb29ba25cf64"
    const val CHAR_STORE_INTERVAL_UUID = "bdc0591c-2f3a-47c8-9d89-0288d52a6d0d"

    /** Byte limits as defined by the contract (UTF-8 byte counts, not chars). */
    const val MAX_SSID_BYTES = 32
    const val MAX_PASSWORD_BYTES = 64

    /** Single byte written to the apply characteristic: store in NVS and reconnect. */
    const val APPLY_COMMAND = 0x01.toByte()

    /** Fixed status tokens emitted on the status characteristic (read + notify). */
    const val STATUS_IDLE = "idle"
    const val STATUS_CONNECTING = "connecting"
    const val STATUS_CONNECTED_PREFIX = "connected:" // connected:<ssid>:<ipv4>
    const val STATUS_FAILED_PREFIX = "failed:" // failed:<code>[:<detail>]

    /** Stable failure codes after [STATUS_FAILED_PREFIX]; optional detail after a further ':'. */
    const val FAIL_AUTH = "auth"
    const val FAIL_NOT_FOUND = "notfound"
    const val FAIL_TIMEOUT = "timeout"
    const val FAIL_INVALID = "invalid"
    const val FAIL_INTERNAL = "internal"
}
