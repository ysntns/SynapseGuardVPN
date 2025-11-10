package com.synapseguard.vpn.domain.model

data class VpnSettings(
    val autoConnect: Boolean = false,
    val killSwitch: Boolean = false,
    val splitTunneling: Boolean = false,
    val excludedApps: Set<String> = emptySet(),
    val preferredProtocol: VpnProtocol = VpnProtocol.WIREGUARD,
    val customDns: List<String> = emptyList()
)
