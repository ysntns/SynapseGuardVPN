package com.synapseguard.vpn.service.core

import android.os.ParcelFileDescriptor
import com.synapseguard.vpn.service.core.ConnectionStats

interface VpnProtocolHandler {
    suspend fun connect(
        serverAddress: String,
        serverPort: Int,
        config: String,
        vpnInterface: ParcelFileDescriptor
    ): Result<Unit>

    suspend fun disconnect(): Result<Unit>

    fun isConnected(): Boolean

    suspend fun getStats(): ConnectionStats
}
