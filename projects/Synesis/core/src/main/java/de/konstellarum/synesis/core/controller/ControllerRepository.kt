package de.konstellarum.synesis.core.controller

import kotlinx.coroutines.flow.StateFlow

/**
 * A BLE peripheral found during scanning. Only devices that advertise the
 * provisioning service are surfaced by real implementations.
 */
data class ControllerDevice(
    val id: String,
    val name: String?,
    val rssi: Int,
)

/**
 * Connection state of the app towards the controller.
 */
sealed interface ControllerLink {
    data object Idle : ControllerLink
    data object Scanning : ControllerLink
    data object Connecting : ControllerLink

    data class Connected(
        val device: ControllerDevice,
        val currentSsid: String,
    ) : ControllerLink

    data class Failed(val message: String) : ControllerLink
}

/**
 * Contract for a controller source. Implementations talk to the sensor controller
 * over BLE: scan, connect, read the current WLAN SSID, and write a new
 * SSID/password pair (see [ProvisioningProtocol]).
 */
interface ControllerRepository {

    val link: StateFlow<ControllerLink>
    val devices: StateFlow<List<ControllerDevice>>
    val provisioningStatus: StateFlow<ProvisioningStatus>

    suspend fun startScan()
    suspend fun stopScan()
    suspend fun connect(device: ControllerDevice)
    suspend fun disconnect()

    /**
     * Writes SSID, password and the apply command to the connected controller.
     * Progress is reported through [provisioningStatus]; terminal states are
     * [ProvisioningStatus.Connected] and [ProvisioningStatus.Failed].
     */
    suspend fun provision(config: WifiConfig)
}
