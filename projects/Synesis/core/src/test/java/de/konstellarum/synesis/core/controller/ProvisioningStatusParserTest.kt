package de.konstellarum.synesis.core.controller

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ProvisioningStatusParserTest {

    @Test
    fun `parses idle`() {
        assertIs<ProvisioningStatus.Idle>(ProvisioningStatusParser.parse("idle"))
    }

    @Test
    fun `parses connecting`() {
        assertIs<ProvisioningStatus.Connecting>(ProvisioningStatusParser.parse("connecting"))
    }

    @Test
    fun `parses connected with ssid and ip`() {
        val status = ProvisioningStatusParser.parse("connected:Keller-WLAN:192.168.1.5")
        val connected = assertIs<ProvisioningStatus.Connected>(status)
        assertEquals("Keller-WLAN", connected.ssid)
        assertEquals("192.168.1.5", connected.ip)
    }

    @Test
    fun `parses connected when the ssid contains colons`() {
        val status = ProvisioningStatusParser.parse("connected:Keller:WLAN:192.168.1.5")
        val connected = assertIs<ProvisioningStatus.Connected>(status)
        assertEquals("Keller:WLAN", connected.ssid)
        assertEquals("192.168.1.5", connected.ip)
    }

    @Test
    fun `parses failed with a bare code`() {
        val status = ProvisioningStatusParser.parse("failed:auth")
        val failed = assertIs<ProvisioningStatus.Failed>(status)
        assertEquals("auth", failed.code)
        assertNull(failed.detail)
    }

    @Test
    fun `parses failed with code and detail`() {
        val status = ProvisioningStatusParser.parse("failed:auth:WLAN_REASON_15")
        val failed = assertIs<ProvisioningStatus.Failed>(status)
        assertEquals("auth", failed.code)
        assertEquals("WLAN_REASON_15", failed.detail)
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertIs<ProvisioningStatus.Connecting>(ProvisioningStatusParser.parse("  connecting\r\n"))
    }

    @Test
    fun `surfaces unknown tokens instead of guessing`() {
        val status = ProvisioningStatusParser.parse("RANDOM")
        val unknown = assertIs<ProvisioningStatus.Unknown>(status)
        assertEquals("RANDOM", unknown.token)
    }

    @Test
    fun `empty payload is unknown`() {
        assertIs<ProvisioningStatus.Unknown>(ProvisioningStatusParser.parse(""))
    }

    @Test
    fun `malformed connected without an ip is unknown`() {
        assertIs<ProvisioningStatus.Unknown>(ProvisioningStatusParser.parse("connected:Keller-WLAN"))
    }

    @Test
    fun `ignores a trailing NUL byte on idle`() {
        assertIs<ProvisioningStatus.Idle>(ProvisioningStatusParser.parse("idle\u0000"))
    }

    @Test
    fun `ignores a trailing NUL byte on connected`() {
        val status = ProvisioningStatusParser.parse("connected:Keller:WLAN:192.168.1.5\u0000")
        val connected = assertIs<ProvisioningStatus.Connected>(status)
        assertEquals("Keller:WLAN", connected.ssid)
        assertEquals("192.168.1.5", connected.ip)
    }

    @Test
    fun `ignores a trailing NUL byte on failed`() {
        val status = ProvisioningStatusParser.parse("failed:auth\u0000")
        val failed = assertIs<ProvisioningStatus.Failed>(status)
        assertEquals("auth", failed.code)
        assertNull(failed.detail)
    }

    @Test
    fun `ignores surrounding whitespace and a trailing NUL byte`() {
        assertIs<ProvisioningStatus.Connecting>(
            ProvisioningStatusParser.parse("  connecting\r\n\u0000"),
        )
    }
}
