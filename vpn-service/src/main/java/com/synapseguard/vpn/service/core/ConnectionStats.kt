package com.synapseguard.vpn.service.core

data class ConnectionStats(
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val packetsSent: Long = 0L,
    val duration: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val downloadSpeedBps: Long = 0L,
    val uploadSpeedBps: Long = 0L,
    val sessionStartTime: Long = 0L
) {
    val totalBytes: Long
        get() = bytesReceived + bytesSent

    val totalPackets: Long
        get() = packetsReceived + packetsSent

    val downloadSpeedMbps: Double
        get() = (downloadSpeedBps * 8.0) / (1024 * 1024)

    val uploadSpeedMbps: Double
        get() = (uploadSpeedBps * 8.0) / (1024 * 1024)
}
