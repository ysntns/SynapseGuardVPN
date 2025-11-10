package com.synapseguard.vpn.domain.model

data class VpnServer(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val ipAddress: String,
    val port: Int,
    val protocol: VpnProtocol,
    val latency: Int = 0,
    val load: Int = 0, // Server load percentage
    val isPremium: Boolean = false,
    val config: String = "" // Protocol-specific configuration
)
