package com.synapseguard.vpn.service.core

import android.os.ParcelFileDescriptor

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

data class ConnectionStats(
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val packetsSent: Long = 0L
)
