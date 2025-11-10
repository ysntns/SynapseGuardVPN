package com.synapseguard.vpn.domain.model

sealed class VpnState {
    object Idle : VpnState()
    object Connecting : VpnState()
    data class Connected(val server: VpnServer, val connectedAt: Long) : VpnState()
    object Disconnecting : VpnState()
    data class Error(val message: String, val throwable: Throwable? = null) : VpnState()
}
