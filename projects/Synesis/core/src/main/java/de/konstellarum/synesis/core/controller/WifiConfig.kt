package de.konstellarum.synesis.core.controller

/**
 * WLAN configuration submitted to the controller. The password is never persisted
 * by the app — it is only written to the device.
 */
data class WifiConfig(
    val ssid: String,
    val password: String,
)
