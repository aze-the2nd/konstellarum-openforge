package de.konstellarum.synesis.core.controller

/**
 * Provisioning progress as reported by the controller on the status
 * characteristic, following [ProvisioningProtocol].
 */
sealed interface ProvisioningStatus {

    /** Initial state before any command was sent (also reported on boot while idle). */
    data object Idle : ProvisioningStatus

    /** The controller is connecting to the configured network. */
    data object Connecting : ProvisioningStatus

    /** The controller joined the network: carries SSID and IPv4 address. */
    data class Connected(val ssid: String, val ip: String) : ProvisioningStatus

    /**
     * The controller rejected the configuration or the reconnect failed.
     * [code] is a stable token from [ProvisioningProtocol] (auth, notfound,
     * timeout, invalid, internal); [detail] is optional free text.
     */
    data class Failed(val code: String, val detail: String? = null) : ProvisioningStatus

    /** A status token was received that this parser does not know. */
    data class Unknown(val token: String) : ProvisioningStatus
}

/**
 * Parses the status tokens defined in [ProvisioningProtocol].
 *
 * Parsing notes pinned by the contract:
 *  - tokens are lowercase and may be surrounded by whitespace;
 *  - a trailing NUL byte is tolerated defensively (historic firmware
 *    behavior: `NimBLECharacteristic::setValue(const char*)` appended the
 *    string terminator; fixed firmware-side, hardened here against
 *    regressions);
 *  - `connected:` is followed by `<ssid>:<ipv4>`; the SSID may itself contain
 *    colons, so the IPv4 address is taken as the LAST colon-separated segment
 *    (the contract guarantees IPv4 in v1);
 *  - `failed:` is followed by a stable code and, optionally, `:<detail>`.
 */
object ProvisioningStatusParser {

    private const val NUL_BYTE = '\u0000'

    fun parse(payload: String): ProvisioningStatus {
        val token = payload.trim().trimEnd(NUL_BYTE).trim()
        return when {
            token == ProvisioningProtocol.STATUS_IDLE -> ProvisioningStatus.Idle
            token == ProvisioningProtocol.STATUS_CONNECTING -> ProvisioningStatus.Connecting
            token.startsWith(ProvisioningProtocol.STATUS_CONNECTED_PREFIX) -> parseConnected(token)
            token.startsWith(ProvisioningProtocol.STATUS_FAILED_PREFIX) -> parseFailed(token)
            else -> ProvisioningStatus.Unknown(token)
        }
    }

    private fun parseConnected(token: String): ProvisioningStatus {
        val body = token.removePrefix(ProvisioningProtocol.STATUS_CONNECTED_PREFIX)
        val separator = body.lastIndexOf(':')
        if (separator <= 0 || separator == body.lastIndex) {
            return ProvisioningStatus.Unknown(token)
        }
        val ssid = body.substring(0, separator)
        val ip = body.substring(separator + 1)
        return ProvisioningStatus.Connected(ssid, ip)
    }

    private fun parseFailed(token: String): ProvisioningStatus {
        val body = token.removePrefix(ProvisioningProtocol.STATUS_FAILED_PREFIX)
        val separator = body.indexOf(':')
        return if (separator <= 0) {
            ProvisioningStatus.Failed(body, null)
        } else {
            ProvisioningStatus.Failed(body.substring(0, separator), body.substring(separator + 1))
        }
    }
}
