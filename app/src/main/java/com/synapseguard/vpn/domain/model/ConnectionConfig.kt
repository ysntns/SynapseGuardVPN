package com.synapseguard.vpn.domain.model

data class ConnectionConfig(
    val server: VpnServer,
    val enableKillSwitch: Boolean = false,
    val enableSplitTunneling: Boolean = false,
    val excludedApps: List<String> = emptyList(),
    val dns: List<String> = listOf("1.1.1.1", "1.0.0.1")
)
