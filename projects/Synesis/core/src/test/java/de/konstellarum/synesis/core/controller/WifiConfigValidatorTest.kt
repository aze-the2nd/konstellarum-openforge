package de.konstellarum.synesis.core.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WifiConfigValidatorTest {

    @Test
    fun `accepts a typical WPA2 configuration`() {
        assertIs<WifiConfigValidator.ValidationResult.Valid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", "geheim1234")),
        )
    }

    @Test
    fun `accepts an open network with empty password`() {
        assertIs<WifiConfigValidator.ValidationResult.Valid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", "")),
        )
    }

    @Test
    fun `rejects an empty SSID`() {
        assertEquals(
            "SSID darf nicht leer sein.",
            (WifiConfigValidator.validate(WifiConfig("", "geheim1234")) as
                WifiConfigValidator.ValidationResult.Invalid).message,
        )
    }

    @Test
    fun `rejects an SSID longer than 32 bytes`() {
        val longSsid = "a".repeat(WifiConfigValidator.MAX_SSID_BYTES + 1)
        assertIs<WifiConfigValidator.ValidationResult.Invalid>(
            WifiConfigValidator.validate(WifiConfig(longSsid, "geheim1234")),
        )
    }

    @Test
    fun `accepts an SSID of exactly 32 bytes`() {
        val ssid = "a".repeat(WifiConfigValidator.MAX_SSID_BYTES)
        assertIs<WifiConfigValidator.ValidationResult.Valid>(
            WifiConfigValidator.validate(WifiConfig(ssid, "geheim1234")),
        )
    }

    @Test
    fun `rejects SSID with control characters`() {
        assertIs<WifiConfigValidator.ValidationResult.Invalid>(
            WifiConfigValidator.validate(WifiConfig("Keller\u0007WLAN", "geheim1234")),
        )
    }

    @Test
    fun `rejects a password longer than 64 bytes`() {
        val longPassword = "p".repeat(WifiConfigValidator.MAX_PASSWORD_BYTES + 1)
        assertIs<WifiConfigValidator.ValidationResult.Invalid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", longPassword)),
        )
    }

    @Test
    fun `accepts a password of exactly 64 bytes`() {
        val password = "p".repeat(WifiConfigValidator.MAX_PASSWORD_BYTES)
        assertIs<WifiConfigValidator.ValidationResult.Valid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", password)),
        )
    }

    @Test
    fun `accepts a short password below the WPA2 minimum`() {
        // Contract allows 0..64 bytes; the access point decides at connect time.
        assertIs<WifiConfigValidator.ValidationResult.Valid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", "kurz")),
        )
    }

    @Test
    fun `rejects password with control characters`() {
        assertIs<WifiConfigValidator.ValidationResult.Invalid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", "geheim\u00071234")),
        )
    }

    @Test
    fun `counts UTF-8 bytes for the SSID limit`() {
        // 'ä' is 2 bytes in UTF-8: 30 x 'a' + 'ä' = 32 bytes -> still valid.
        val ssid = "a".repeat(30) + "ä"
        assertIs<WifiConfigValidator.ValidationResult.Valid>(
            WifiConfigValidator.validate(WifiConfig(ssid, "geheim1234")),
        )
        // 31 x 'a' + 'ä' = 33 bytes -> invalid.
        val tooLong = "a".repeat(31) + "ä"
        assertIs<WifiConfigValidator.ValidationResult.Invalid>(
            WifiConfigValidator.validate(WifiConfig(tooLong, "geheim1234")),
        )
    }

    @Test
    fun `counts UTF-8 bytes for the password limit`() {
        // 63 x 'p' + 'ä' = 65 bytes -> invalid.
        val tooLong = "p".repeat(63) + "ä"
        assertIs<WifiConfigValidator.ValidationResult.Invalid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", tooLong)),
        )
        // 62 x 'p' + 'ä' = 64 bytes -> valid.
        val boundary = "p".repeat(62) + "ä"
        assertIs<WifiConfigValidator.ValidationResult.Valid>(
            WifiConfigValidator.validate(WifiConfig("Keller-WLAN", boundary)),
        )
    }
}
