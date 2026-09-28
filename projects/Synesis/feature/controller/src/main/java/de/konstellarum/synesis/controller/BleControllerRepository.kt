package de.konstellarum.synesis.controller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import de.konstellarum.synesis.core.controller.ControllerDevice
import de.konstellarum.synesis.core.controller.ControllerLink
import de.konstellarum.synesis.core.controller.ControllerRepository
import de.konstellarum.synesis.core.controller.ProvisioningProtocol
import de.konstellarum.synesis.core.controller.ProvisioningStatus
import de.konstellarum.synesis.core.controller.ProvisioningStatusParser
import de.konstellarum.synesis.core.controller.WifiConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * BLE implementation of [ControllerRepository] against the provisioning service
 * defined in [ProvisioningProtocol]. All Android BLE calls are guarded by
 * runtime permission checks; state is surfaced through the StateFlows.
 */
@SuppressLint("MissingPermission")
class BleControllerRepository(
    private val context: Context,
) : ControllerRepository {

    private val bluetoothManager: BluetoothManager?
        get() = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val _link = MutableStateFlow<ControllerLink>(ControllerLink.Idle)
    override val link: StateFlow<ControllerLink> = _link.asStateFlow()

    private val _devices = MutableStateFlow<List<ControllerDevice>>(emptyList())
    override val devices: StateFlow<List<ControllerDevice>> = _devices.asStateFlow()

    private val _provisioningStatus =
        MutableStateFlow<ProvisioningStatus>(ProvisioningStatus.Idle)
    override val provisioningStatus: StateFlow<ProvisioningStatus> =
        _provisioningStatus.asStateFlow()

    private val serviceUuid = UUID.fromString(ProvisioningProtocol.SERVICE_UUID)
    private val ssidUuid = UUID.fromString(ProvisioningProtocol.CHAR_SSID_UUID)
    private val passwordUuid = UUID.fromString(ProvisioningProtocol.CHAR_PASSWORD_UUID)
    private val applyUuid = UUID.fromString(ProvisioningProtocol.CHAR_APPLY_UUID)
    private val statusUuid = UUID.fromString(ProvisioningProtocol.CHAR_STATUS_UUID)

    private var scanCallback: ScanCallback? = null
    private var gatt: BluetoothGatt? = null
    private var gattCallbacks: GattCallbacks? = null
    private var ssidCharacteristic: BluetoothGattCharacteristic? = null
    private var passwordCharacteristic: BluetoothGattCharacteristic? = null
    private var applyCharacteristic: BluetoothGattCharacteristic? = null
    private var statusCharacteristic: BluetoothGattCharacteristic? = null

    private var provisioningOutcome: CompletableDeferred<ProvisioningStatus>? = null

    override suspend fun startScan() {
        stopScan()
        val adapter = adapterOrFail() ?: return
        if (!hasBluetoothPermission()) {
            fail("Bluetooth-Berechtigung fehlt.")
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            fail("BLE-Scanner nicht verfügbar.")
            return
        }
        _devices.value = emptyList()
        _link.value = ControllerLink.Scanning
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val entry = ControllerDevice(
                    id = device.address,
                    name = result.scanRecord?.deviceName ?: device.name,
                    rssi = result.rssi,
                )
                _devices.value = (_devices.value.filterNot { it.id == entry.id } + entry)
                    .sortedByDescending { it.rssi }
            }

            override fun onScanFailed(errorCode: Int) {
                fail("Scan fehlgeschlagen (Code $errorCode).")
            }
        }
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    override suspend fun stopScan() {
        runCatching {
            bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
        scanCallback = null
        if (_link.value == ControllerLink.Scanning) {
            _link.value = ControllerLink.Idle
        }
    }

    override suspend fun connect(device: ControllerDevice) {
        stopScan()
        closeGatt()
        val adapter = adapterOrFail() ?: return
        if (!hasBluetoothPermission()) {
            fail("Bluetooth-Berechtigung fehlt.")
            return
        }
        val remote = adapter.getRemoteDevice(device.id)
            ?: run {
                fail("Gerät nicht erreichbar.")
                return
            }

        _link.value = ControllerLink.Connecting

        val callbacks = GattCallbacks()
        // Attempt-local await: a late DISCONNECTED callback from a previous,
        // already closed gatt must not resolve the new attempt.
        val attemptAwait = CompletableDeferred<BluetoothGatt?>()
        callbacks.onConnected = { connectedGatt ->
            // Request a larger MTU so 32/64-byte payloads fit a single write.
            // If the exchange fails or the controller declines, GATT long
            // writes (prepare/execute) still carry the payload.
            runCatching { connectedGatt.requestMtu(MTU_TARGET) }
            attemptAwait.complete(connectedGatt)
        }
        callbacks.onDisconnected = { attemptAwait.complete(null) }

        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            remote.connectGatt(context, false, callbacks, BluetoothDevice.TRANSPORT_LE)
        } else {
            @Suppress("DEPRECATION")
            remote.connectGatt(context, false, callbacks)
        }
        if (created == null) {
            fail("Verbindungsaufbau fehlgeschlagen.")
            return
        }

        val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) { attemptAwait.await() }
        if (connected == null) {
            runCatching { created.close() }
            fail("Zeitüberschreitung beim Verbinden.")
            return
        }

        gatt = connected
        gattCallbacks = callbacks
        callbacks.onConnected = null
        // From here on, only disconnect events of the active gatt count.
        callbacks.onDisconnected = { disconnectedGatt ->
            if (disconnectedGatt === gatt) onDisconnectEvent()
        }

        val servicesOk = withTimeoutOrNull(SERVICES_TIMEOUT_MILLIS) {
            val deferred = CompletableDeferred<Boolean>()
            callbacks.onServicesDiscovered = { _, status ->
                deferred.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
            connected.discoverServices()
            deferred.await()
        } ?: false
        if (!servicesOk) {
            fail("Dienste des Controllers konnten nicht gelesen werden.")
            return
        }

        val service = connected.services.firstOrNull { it.uuid == serviceUuid }
        if (service == null) {
            fail("Provisioning-Dienst nicht gefunden — läuft die passende Firmware auf dem Controller?")
            return
        }

        ssidCharacteristic = service.getCharacteristic(ssidUuid)
        passwordCharacteristic = service.getCharacteristic(passwordUuid)
        applyCharacteristic = service.getCharacteristic(applyUuid)
        statusCharacteristic = service.getCharacteristic(statusUuid)

        val writeUuids = listOf(ssidCharacteristic, passwordCharacteristic, applyCharacteristic)
        if (writeUuids.any { it == null || (it.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 }) {
            fail("Schreib-Merkmale des Controllers unvollständig.")
            return
        }
        val statusChar = statusCharacteristic
        if (statusChar == null) {
            fail("Status-Merkmal des Controllers fehlt.")
            return
        }

        callbacks.onCharacteristicChanged = { _, characteristic ->
            onStatusNotification(characteristic)
        }

        val notifyOk = withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) {
            val deferred = CompletableDeferred<Boolean>()
            callbacks.onDescriptorWrite = { _, descriptor, status ->
                if (descriptor.uuid == CCCD_UUID) {
                    deferred.complete(status == BluetoothGatt.GATT_SUCCESS)
                }
            }
            connected.setCharacteristicNotification(statusChar, true)
            val cccd = statusChar.getDescriptor(CCCD_UUID)
            if (cccd == null) {
                deferred.complete(true)
            } else {
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                connected.writeDescriptor(cccd)
            }
            deferred.await()
        } ?: false
        if (!notifyOk) {
            fail("Status-Benachrichtigung konnte nicht aktiviert werden.")
            return
        }

        // The contract has no dedicated current-SSID characteristic; the
        // current WLAN is derived from the initial status read
        // (connected:<ssid>:<ip>) or shown as unknown while idle.
        val initialStatus = readCharacteristic(connected, callbacks, statusCharacteristic)
            ?.let { ProvisioningStatusParser.parse(String(it, Charsets.UTF_8)) }
            ?: ProvisioningStatus.Idle
        val currentSsid = (initialStatus as? ProvisioningStatus.Connected)?.ssid.orEmpty()

        _provisioningStatus.value = initialStatus
        _link.value = ControllerLink.Connected(device, currentSsid)
    }

    override suspend fun disconnect() {
        stopScan()
        closeGatt()
        _provisioningStatus.value = ProvisioningStatus.Idle
        _link.value = ControllerLink.Idle
    }

    override suspend fun provision(config: WifiConfig) {
        val g = gatt ?: run {
            fail("Keine Verbindung zum Controller.")
            return
        }
        val callbacks = gattCallbacks ?: run {
            fail("Keine Verbindung zum Controller.")
            return
        }
        val ssidChar = ssidCharacteristic
        val passwordChar = passwordCharacteristic
        val applyChar = applyCharacteristic
        if (ssidChar == null || passwordChar == null || applyChar == null) {
            fail("Schreib-Merkmale des Controllers unvollständig.")
            return
        }

        _provisioningStatus.value = ProvisioningStatus.Idle

        if (!writeCharacteristic(g, callbacks, ssidChar, config.ssid.toByteArray(Charsets.UTF_8))) {
            fail("SSID konnte nicht geschrieben werden.")
            return
        }
        if (!writeCharacteristic(
                g,
                callbacks,
                passwordChar,
                config.password.toByteArray(Charsets.UTF_8),
            )
        ) {
            fail("Passwort konnte nicht geschrieben werden.")
            return
        }

        provisioningOutcome = CompletableDeferred()
        if (!writeCharacteristic(
                g,
                callbacks,
                applyChar,
                byteArrayOf(ProvisioningProtocol.APPLY_COMMAND),
            )
        ) {
            provisioningOutcome = null
            fail("Befehl konnte nicht gesendet werden.")
            return
        }

        val outcome = withTimeoutOrNull(PROVISIONING_TIMEOUT_MILLIS) {
            provisioningOutcome?.await()
        } ?: ProvisioningStatus.Failed(ProvisioningProtocol.FAIL_TIMEOUT)
        provisioningOutcome = null
        _provisioningStatus.value = outcome
    }

    private fun onStatusNotification(characteristic: BluetoothGattCharacteristic) {
        val payload = characteristic.value ?: return
        val status = ProvisioningStatusParser.parse(String(payload, Charsets.UTF_8))
        _provisioningStatus.value = status
        if (status is ProvisioningStatus.Connected || status is ProvisioningStatus.Failed) {
            provisioningOutcome?.complete(status)
            provisioningOutcome = null
        }
    }

    private fun onDisconnectEvent() {
        provisioningOutcome?.complete(ProvisioningStatus.Failed(CODE_LINK_LOST))
        provisioningOutcome = null
        if (_link.value is ControllerLink.Connecting) {
            fail("Verbindung zum Controller fehlgeschlagen.")
        } else {
            fail("Verbindung zum Controller unterbrochen.")
        }
    }

    private suspend fun writeCharacteristic(
        g: BluetoothGatt,
        callbacks: GattCallbacks,
        characteristic: BluetoothGattCharacteristic,
        payload: ByteArray,
    ): Boolean = withTimeoutOrNull(WRITE_TIMEOUT_MILLIS) {
        val deferred = CompletableDeferred<Boolean>()
        callbacks.onCharacteristicWrite = { _, written, status ->
            if (written.uuid == characteristic.uuid) {
                deferred.complete(status == BluetoothGatt.GATT_SUCCESS)
            }
        }
        characteristic.value = payload
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        if (!g.writeCharacteristic(characteristic)) {
            deferred.complete(false)
        }
        deferred.await()
    } ?: false

    private suspend fun readCharacteristic(
        g: BluetoothGatt,
        callbacks: GattCallbacks,
        characteristic: BluetoothGattCharacteristic?,
    ): ByteArray? {
        if (characteristic == null) return null
        return withTimeoutOrNull(READ_TIMEOUT_MILLIS) {
            val deferred = CompletableDeferred<ByteArray?>()
            callbacks.onCharacteristicRead = { _, read, status ->
                if (read.uuid == characteristic.uuid) {
                    deferred.complete(
                        if (status == BluetoothGatt.GATT_SUCCESS) read.value else null,
                    )
                }
            }
            if (!g.readCharacteristic(characteristic)) {
                deferred.complete(null)
            }
            deferred.await()
        }
    }

    private fun closeGatt() {
        runCatching { gatt?.close() }
        gatt = null
        gattCallbacks = null
        ssidCharacteristic = null
        passwordCharacteristic = null
        applyCharacteristic = null
        statusCharacteristic = null
        provisioningOutcome = null
    }

    private fun adapterOrFail(): BluetoothAdapter? {
        val adapter = bluetoothManager?.adapter
        if (adapter == null) {
            fail("Bluetooth ist auf diesem Gerät nicht verfügbar.")
            return null
        }
        if (!adapter.isEnabled) {
            fail("Bluetooth ist ausgeschaltet.")
            return null
        }
        return adapter
    }

    private fun hasBluetoothPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun fail(message: String) {
        _link.value = ControllerLink.Failed(message)
    }

    private class GattCallbacks : BluetoothGattCallback() {
        var onConnected: ((BluetoothGatt) -> Unit)? = null
        var onDisconnected: ((BluetoothGatt) -> Unit)? = null
        var onServicesDiscovered: ((BluetoothGatt, Int) -> Unit)? = null
        var onCharacteristicWrite: ((BluetoothGatt, BluetoothGattCharacteristic, Int) -> Unit)? = null
        var onCharacteristicRead: ((BluetoothGatt, BluetoothGattCharacteristic, Int) -> Unit)? = null
        var onDescriptorWrite: ((BluetoothGatt, BluetoothGattDescriptor, Int) -> Unit)? = null
        var onCharacteristicChanged: ((BluetoothGatt, BluetoothGattCharacteristic) -> Unit)? = null

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> onConnected?.invoke(gatt)
                BluetoothProfile.STATE_DISCONNECTED -> onDisconnected?.invoke(gatt)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            onServicesDiscovered?.invoke(gatt, status)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            onCharacteristicWrite?.invoke(gatt, characteristic, status)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            onCharacteristicRead?.invoke(gatt, characteristic, status)
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            onDescriptorWrite?.invoke(gatt, descriptor, status)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            onCharacteristicChanged?.invoke(gatt, characteristic)
        }
    }

    companion object {
        val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val CODE_LINK_LOST = "LINKLOST"
        const val MTU_TARGET = 247
        const val CONNECT_TIMEOUT_MILLIS = 15_000L
        const val SERVICES_TIMEOUT_MILLIS = 15_000L
        const val WRITE_TIMEOUT_MILLIS = 10_000L
        const val READ_TIMEOUT_MILLIS = 10_000L
        const val PROVISIONING_TIMEOUT_MILLIS = 90_000L
    }
}
