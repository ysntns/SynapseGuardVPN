package com.synapseguard.vpn.domain.model

data class ConnectionStats(
    val bytesReceived: Long = 0L,
    val bytesSent: Long = 0L,
    val duration: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalBytes: Long
        get() = bytesReceived + bytesSent
}
