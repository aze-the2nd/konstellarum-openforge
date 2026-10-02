package de.konstellarum.synesis.controller

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.konstellarum.synesis.core.controller.ControllerDevice
import de.konstellarum.synesis.core.controller.ControllerLink
import de.konstellarum.synesis.core.controller.ControllerRepository
import de.konstellarum.synesis.core.controller.ProvisioningStatus
import de.konstellarum.synesis.core.controller.StoreIntervalState
import de.konstellarum.synesis.core.controller.StoreIntervalUpdate
import de.konstellarum.synesis.core.controller.StoreIntervalValidator
import de.konstellarum.synesis.core.controller.WifiConfig
import de.konstellarum.synesis.core.controller.WifiConfigValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Composable
fun ControllerModule(repository: ControllerRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main) }

    val link by repository.link.collectAsState()
    val devices by repository.devices.collectAsState()
    val provisioningStatus by repository.provisioningStatus.collectAsState()
    val storeInterval by repository.storeInterval.collectAsState()

    var hasPermissions by remember { mutableStateOf(checkBluetoothPermissions(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPermissions = result.values.all { granted -> granted }
    }

    var applying by remember { mutableStateOf(false) }
    var applyResult by remember { mutableStateOf<ApplyResult?>(null) }

    // Clean up the BLE session when the module leaves the composition.
    DisposableEffect(repository) {
        onDispose { appScope.launch { repository.disconnect() } }
    }

    fun ensurePermissions(action: () -> Unit) {
        if (hasPermissions) action() else permissionLauncher.launch(requiredPermissions())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Controller", style = MaterialTheme.typography.titleLarge)
        Text(
            "WLAN-Einstellungen des Sensors per Bluetooth ändern.",
            style = MaterialTheme.typography.bodyMedium,
        )

        when (val result = applyResult) {
            is ApplyResult.Success -> SuccessPane(
                onDone = {
                    applyResult = null
                    scope.launch { repository.disconnect() }
                },
            )

            else -> when (val current = link) {
                ControllerLink.Idle -> IdlePane(
                    onScan = {
                        ensurePermissions { scope.launch { repository.startScan() } }
                    },
                )

                ControllerLink.Scanning -> ScanningPane(
                    devices = devices,
                    onSelect = { device ->
                        ensurePermissions { scope.launch { repository.connect(device) } }
                    },
                    onStop = { scope.launch { repository.stopScan() } },
                )

                ControllerLink.Connecting -> ConnectingPane()

                is ControllerLink.Connected -> ProvisioningForm(
                    connected = current,
                    provisioningStatus = provisioningStatus,
                    applying = applying,
                    result = result,
                    storeInterval = storeInterval,
                    onSetInterval = repository::setStoreInterval,
                    onApply = { config ->
                        scope.launch {
                            applying = true
                            applyResult = null
                            repository.provision(config)
                            applying = false
                            applyResult = when (val final = repository.provisioningStatus.value) {
                                is ProvisioningStatus.Connected -> ApplyResult.Success
                                is ProvisioningStatus.Failed -> ApplyResult.Failure(final.userText())
                                else -> ApplyResult.Failure("Keine Rückmeldung vom Controller.")
                            }
                        }
                    },
                    onDisconnect = { scope.launch { repository.disconnect() } },
                )

                is ControllerLink.Failed -> FailedPane(
                    message = current.message,
                    onReset = { scope.launch { repository.disconnect() } },
                )
            }
        }
    }
}

@Composable
private fun IdlePane(onScan: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Der Controller muss eingeschaltet und in Reichweite sein.")
        Button(onClick = onScan) {
            Text("Nach Controller suchen")
        }
    }
}

@Composable
private fun ScanningPane(
    devices: List<ControllerDevice>,
    onSelect: (ControllerDevice) -> Unit,
    onStop: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
            Text("Suche läuft …")
        }
        if (devices.isEmpty()) {
            Text("Noch kein Controller gefunden.")
        }
        devices.forEach { device ->
            Card(onClick = { onSelect(device) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            device.name ?: "Controller",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(device.id, style = MaterialTheme.typography.labelMedium)
                    }
                    Text("${device.rssi} dBm", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        TextButton(onClick = onStop) {
            Text("Abbrechen")
        }
    }
}

@Composable
private fun ConnectingPane() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Text("Verbinde …")
    }
}

@Composable
private fun ProvisioningForm(
    connected: ControllerLink.Connected,
    provisioningStatus: ProvisioningStatus,
    applying: Boolean,
    result: ApplyResult?,
    storeInterval: StoreIntervalState,
    onSetInterval: suspend (Int) -> StoreIntervalUpdate,
    onApply: (WifiConfig) -> Unit,
    onDisconnect: () -> Unit,
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val config = remember(ssid, password) { WifiConfig(ssid.trim(), password) }
    val validation = remember(ssid, password) { WifiConfigValidator.validate(config) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Verbunden mit ${connected.device.name ?: connected.device.id}",
            style = MaterialTheme.typography.labelMedium,
        )
        if (connected.currentSsid.isNotBlank()) {
            Text(
                "Aktuelles WLAN: ${connected.currentSsid}",
                style = MaterialTheme.typography.labelMedium,
            )
        }

        OutlinedTextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("WLAN-Name (SSID)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("WLAN-Passwort") },
            singleLine = true,
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (passwordVisible) {
                            "Passwort verbergen"
                        } else {
                            "Passwort anzeigen"
                        },
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Leer lassen für ein offenes Netz.",
            style = MaterialTheme.typography.labelSmall,
        )

        when (val validationResult = validation) {
            is WifiConfigValidator.ValidationResult.Invalid -> Text(
                validationResult.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )

            else -> Unit
        }

        Button(
            onClick = { onApply(config) },
            enabled = validation is WifiConfigValidator.ValidationResult.Valid && !applying,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (applying) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Übernehmen …")
            } else {
                Text("Übernehmen")
            }
        }

        if (applying) {
            Text(provisioningStatus.userText(), style = MaterialTheme.typography.bodyMedium)
        }
        if (result is ApplyResult.Failure) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    result.message,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        StoreIntervalSection(
            state = storeInterval,
            onSetInterval = onSetInterval,
        )

        TextButton(onClick = onDisconnect) {
            Text("Trennen")
        }
    }
}

@Composable
private fun StoreIntervalSection(
    state: StoreIntervalState,
    onSetInterval: suspend (Int) -> StoreIntervalUpdate,
) {
    val scope = rememberCoroutineScope()
    var intervalText by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<StoreIntervalUpdate?>(null) }

    val validation = remember(intervalText) { StoreIntervalValidator.validate(intervalText) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Speicherintervall", style = MaterialTheme.typography.titleMedium)
        Text(
            "Wie oft der Controller Messwerte in seine Datenbank schreibt.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (state is StoreIntervalState.Unsupported) {
            Text(
                "Dieser Controller unterstützt das Speicherintervall nicht (Firmware v3 erforderlich).",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            when (state) {
                is StoreIntervalState.Current -> Text(
                    "Aktuell: ${formatInterval(state.seconds)}",
                    style = MaterialTheme.typography.labelMedium,
                )

                StoreIntervalState.Unknown -> Text(
                    "Aktueller Wert unbekannt.",
                    style = MaterialTheme.typography.labelMedium,
                )

                StoreIntervalState.Unsupported -> Unit
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { raw ->
                        intervalText = raw.filter { it.isDigit() }.take(4)
                    },
                    label = { Text("Intervall") },
                    suffix = { Text("s") },
                    singleLine = true,
                    isError = intervalText.isNotBlank() &&
                        validation is StoreIntervalValidator.ValidationResult.Invalid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val valid = validation as? StoreIntervalValidator.ValidationResult.Valid
                            ?: return@Button
                        scope.launch {
                            busy = true
                            update = null
                            update = onSetInterval(valid.seconds)
                            busy = false
                        }
                    },
                    enabled = !busy && validation is StoreIntervalValidator.ValidationResult.Valid,
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Setzen …")
                    } else {
                        Text("Setzen")
                    }
                }
            }

            (validation as? StoreIntervalValidator.ValidationResult.Invalid)?.let { invalid ->
                if (intervalText.isNotBlank()) {
                    Text(
                        invalid.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(10 to "10 s", 60 to "60 s", 300 to "5 min", 3600 to "60 min")
                    .forEach { (seconds, label) ->
                        TextButton(onClick = { intervalText = seconds.toString() }) {
                            Text(label)
                        }
                    }
            }
        }

        when (val current = update) {
            is StoreIntervalUpdate.Applied -> Text(
                "Übernommen: ${formatInterval(current.seconds)}.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )

            is StoreIntervalUpdate.Rejected -> Text(
                "Vom Controller nicht übernommen — aktuell bleibt ${formatInterval(current.current)}.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

            is StoreIntervalUpdate.Unavailable -> Text(
                current.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )

            null -> Unit
        }
    }
}

private fun formatInterval(seconds: Int): String =
    if (seconds >= 60 && seconds % 60 == 0) "${seconds / 60} min" else "$seconds s"

@Composable
private fun SuccessPane(onDone: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "WLAN-Einstellungen übernommen.",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "Der Controller verbindet sich jetzt mit dem neuen Netz. " +
                    "Die Verbindung zur App kann dabei kurz unterbrochen werden.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDone) {
                Text("Fertig")
            }
        }
    }
}

@Composable
private fun FailedPane(message: String, onReset: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onReset) {
            Text("Erneut versuchen")
        }
    }
}

private sealed interface ApplyResult {
    data object Success : ApplyResult
    data class Failure(val message: String) : ApplyResult
}

private fun ProvisioningStatus.userText(): String = when (this) {
    ProvisioningStatus.Idle -> "Bereit."
    ProvisioningStatus.Connecting -> "Verbindung zum WLAN wird hergestellt …"
    is ProvisioningStatus.Connected -> "Mit dem neuen WLAN verbunden."
    is ProvisioningStatus.Failed -> when (code) {
        "auth" -> "Passwort wurde abgelehnt."
        "notfound" -> "Netz nicht gefunden."
        "invalid" -> "Einstellungen ungültig."
        "timeout" -> "Zeitüberschreitung beim Verbinden."
        "internal" -> "Interner Fehler des Controllers."
        "LINKLOST" -> "Verbindung zum Controller unterbrochen."
        else -> "Fehler: $code"
    }

    is ProvisioningStatus.Unknown -> "Unbekannte Rückmeldung: ${token}"
}

private fun requiredPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

private fun checkBluetoothPermissions(context: Context): Boolean =
    requiredPermissions().all { permission ->
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
