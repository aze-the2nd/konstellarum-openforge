package de.konstellarum.synesis.core.controller

/**
 * Validates WLAN settings before they are written to the controller.
 *
 * Limits follow the BLE provisioning contract ([ProvisioningProtocol]):
 * SSID 1..32 bytes UTF-8, password 0..64 bytes UTF-8 (empty means open
 * network). The app is deliberately permissive: WPA2's 8..63 byte rule is
 * enforced by the access point / controller at connect time, and the
 * controller reports the outcome via `failed:<code>`.
 */
object WifiConfigValidator {

    const val MAX_SSID_BYTES = ProvisioningProtocol.MAX_SSID_BYTES
    const val MAX_PASSWORD_BYTES = ProvisioningProtocol.MAX_PASSWORD_BYTES

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val message: String) : ValidationResult
    }

    fun validate(config: WifiConfig): ValidationResult {
        val ssidBytes = config.ssid.toByteArray(Charsets.UTF_8)
        if (ssidBytes.isEmpty()) {
            return ValidationResult.Invalid("SSID darf nicht leer sein.")
        }
        if (ssidBytes.size > MAX_SSID_BYTES) {
            return ValidationResult.Invalid("SSID ist zu lang (max. $MAX_SSID_BYTES Bytes).")
        }
        if (config.ssid.any { it.code < 0x20 || it.code == 0x7F }) {
            return ValidationResult.Invalid("SSID enthält ungültige Zeichen.")
        }

        val passwordBytes = config.password.toByteArray(Charsets.UTF_8)
        if (passwordBytes.size > MAX_PASSWORD_BYTES) {
            return ValidationResult.Invalid(
                "Passwort ist zu lang (max. $MAX_PASSWORD_BYTES Bytes).",
            )
        }
        if (config.password.any { it.code < 0x20 || it.code == 0x7F }) {
            return ValidationResult.Invalid("Passwort enthält ungültige Zeichen.")
        }
        return ValidationResult.Valid
    }
}
