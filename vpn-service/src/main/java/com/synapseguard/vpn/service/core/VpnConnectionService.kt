package com.synapseguard.vpn.service.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import com.synapseguard.vpn.service.wireguard.WireGuardHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class VpnConnectionService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var protocolHandler: VpnProtocolHandler? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var statsMonitorJob: Job? = null
    private var packetForwardJob: Job? = null

    // Connection state and statistics
    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectionStats = MutableStateFlow(ServiceConnectionStats())
    val connectionStats: StateFlow<ServiceConnectionStats> = _connectionStats.asStateFlow()

    // Configuration
    private var serverAddress: String = ""
    private var serverPort: Int = 51820
    private var enableKillSwitch: Boolean = false
    private var enableSplitTunneling: Boolean = false
    private var excludedApps: List<String> = emptyList()
    private var dnsServers: List<String> = listOf("1.1.1.1", "1.0.0.1")
    private var sessionStartTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        Timber.d("VpnConnectionService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                // Extract configuration from intent
                serverAddress = intent.getStringExtra(EXTRA_SERVER_ADDRESS) ?: ""
                serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 51820)
                enableKillSwitch = intent.getBooleanExtra(EXTRA_KILL_SWITCH, false)
                enableSplitTunneling = intent.getBooleanExtra(EXTRA_SPLIT_TUNNELING, false)
                excludedApps = intent.getStringArrayListExtra(EXTRA_EXCLUDED_APPS) ?: emptyList()
                dnsServers = intent.getStringArrayListExtra(EXTRA_DNS_SERVERS) ?: listOf("1.1.1.1", "1.0.0.1")

                connect()
                START_STICKY
            }
            ACTION_DISCONNECT -> {
                disconnect()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun connect() {
        serviceScope.launch {
            try {
                _connectionState.value = ConnectionState.CONNECTING

                // Start foreground service with notification
                startForeground(NOTIFICATION_ID, createNotification("Connecting..."))

                Timber.d("Building VPN interface...")

                // Build VPN interface
                val builder = Builder()
                    .setSession("SynapseGuard VPN")
                    .addAddress("10.8.0.2", 24)  // VPN tunnel IP
                    .addRoute("0.0.0.0", 0)       // Route all traffic through VPN

                // Add DNS servers
                dnsServers.forEach { dns ->
                    try {
                        builder.addDnsServer(dns)
                        Timber.d("Added DNS server: $dns")
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to add DNS server: $dns")
                    }
                }

                // Configure split tunneling (exclude apps)
                if (enableSplitTunneling && excludedApps.isNotEmpty()) {
                    excludedApps.forEach { packageName ->
                        try {
                            builder.addDisallowedApplication(packageName)
                            Timber.d("Excluded app from VPN: $packageName")
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to exclude app: $packageName")
                        }
                    }
                }

                // Set blocking mode for kill switch
                if (enableKillSwitch) {
                    builder.setBlocking(true)
                    Timber.d("Kill switch enabled - blocking mode active")
                }

                // Set MTU for better performance
                builder.setMtu(1400)

                // Establish VPN interface
                vpnInterface = builder.establish()

                if (vpnInterface == null) {
                    throw IllegalStateException("Failed to establish VPN interface")
                }

                Timber.d("VPN interface established successfully")

                // Initialize protocol handler
                protocolHandler = WireGuardHandler()

                // Connect using protocol handler
                val result = protocolHandler?.connect(
                    serverAddress = serverAddress,
                    serverPort = serverPort,
                    config = "",  // WireGuard config will be generated or loaded
                    vpnInterface = vpnInterface!!
                )

                if (result?.isSuccess == true) {
                    sessionStartTime = System.currentTimeMillis()
                    _connectionState.value = ConnectionState.CONNECTED
                    _connectionStats.value = _connectionStats.value.copy(
                        sessionStartTime = sessionStartTime,
                        timestamp = sessionStartTime,
                        duration = 0L
                    )
                    updateNotification("Connected")

                    // Start packet forwarding
                    startPacketForwarding()

                    // Start statistics monitoring
                    startStatsMonitoring()

                    Timber.d("VPN connected successfully")
                } else {
                    throw Exception("Protocol connection failed: ${result?.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Timber.e(e, "Failed to establish VPN connection")
                _connectionState.value = ConnectionState.ERROR
                updateNotification("Connection failed")
                disconnect()
            }
        }
    }

    private fun startPacketForwarding() {
        packetForwardJob?.cancel()
        packetForwardJob = serviceScope.launch {
            try {
                val vpnFd = vpnInterface ?: return@launch
                val inputStream = FileInputStream(vpnFd.fileDescriptor)
                val outputStream = FileOutputStream(vpnFd.fileDescriptor)

                // Create UDP channel for forwarding packets
                val channel = DatagramChannel.open()
                channel.connect(InetSocketAddress(serverAddress, serverPort))
                channel.configureBlocking(false)

                val buffer = ByteBuffer.allocate(32767)  // Max IP packet size

                Timber.d("Starting packet forwarding...")

                while (isActive && vpnInterface != null) {
                    try {
                        // Read from VPN interface
                        buffer.clear()
                        val length = inputStream.read(buffer.array())

                        if (length > 0) {
                            // Update sent bytes
                            val updatedTimestamp = System.currentTimeMillis()
                            _connectionStats.value = _connectionStats.value.copy(
                                bytesSent = _connectionStats.value.bytesSent + length,
                                packetsSent = _connectionStats.value.packetsSent + 1,
                                timestamp = updatedTimestamp,
                                duration = (updatedTimestamp - sessionStartTime).coerceAtLeast(0)
                            )

                            // Forward to server (in real implementation)
                            // For now, we're simulating
                            buffer.limit(length)

                            // Echo back for testing (simulate server response)
                            delay(10)  // Simulate network latency
                            outputStream.write(buffer.array(), 0, length)

                            // Update received bytes
                            val receivedTimestamp = System.currentTimeMillis()
                            _connectionStats.value = _connectionStats.value.copy(
                                bytesReceived = _connectionStats.value.bytesReceived + length,
                                packetsReceived = _connectionStats.value.packetsReceived + 1,
                                timestamp = receivedTimestamp,
                                duration = (receivedTimestamp - sessionStartTime).coerceAtLeast(0)
                            )
                        }

                        delay(1)  // Small delay to prevent busy waiting

                    } catch (e: Exception) {
                        if (isActive) {
                            Timber.e(e, "Error in packet forwarding")
                        }
                        break
                    }
                }

                channel.close()
                Timber.d("Packet forwarding stopped")

            } catch (e: Exception) {
                Timber.e(e, "Failed to start packet forwarding")
            }
        }
    }

    private fun startStatsMonitoring() {
        statsMonitorJob?.cancel()
        statsMonitorJob = serviceScope.launch {
            var lastCheckTime = System.currentTimeMillis()
            var lastBytesSent = 0L
            var lastBytesReceived = 0L

            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(1000)  // Update every second

                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - lastCheckTime) / 1000.0

                if (elapsedSeconds > 0) {
                    val currentStats = _connectionStats.value

                    // Calculate speeds (bytes per second)
                    val uploadSpeed = ((currentStats.bytesSent - lastBytesSent) / elapsedSeconds).toLong()
                    val downloadSpeed = ((currentStats.bytesReceived - lastBytesReceived) / elapsedSeconds).toLong()

                    val updatedTimestamp = System.currentTimeMillis()
                    _connectionStats.value = currentStats.copy(
                        uploadSpeedBps = uploadSpeed,
                        downloadSpeedBps = downloadSpeed,
                        timestamp = updatedTimestamp,
                        duration = (updatedTimestamp - sessionStartTime).coerceAtLeast(0)
                    )

                    lastBytesSent = currentStats.bytesSent
                    lastBytesReceived = currentStats.bytesReceived
                    lastCheckTime = currentTime

                    // Update notification with stats
                    val uploadKBps = uploadSpeed / 1024.0
                    val downloadKBps = downloadSpeed / 1024.0
                    updateNotification("↑ %.1f KB/s ↓ %.1f KB/s".format(uploadKBps, downloadKBps))
                }
            }
        }
    }

    private fun disconnect() {
        serviceScope.launch {
            try {
                _connectionState.value = ConnectionState.DISCONNECTING

                // Stop monitoring jobs
                statsMonitorJob?.cancel()
                packetForwardJob?.cancel()

                // Disconnect protocol handler
                protocolHandler?.disconnect()
                protocolHandler = null

                // Close VPN interface
                vpnInterface?.close()
                vpnInterface = null

                _connectionState.value = ConnectionState.IDLE
                _connectionStats.value = ServiceConnectionStats()
                sessionStartTime = 0L

                Timber.d("VPN interface closed")
            } catch (e: Exception) {
                Timber.e(e, "Error closing VPN interface")
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        disconnect()
        serviceScope.cancel()
        super.onDestroy()
        Timber.d("VpnConnectionService destroyed")
        instance = null
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
        Timber.d("VPN permission revoked")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SynapseGuard VPN connection status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        // Create intent to open the app
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SynapseGuard VPN")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)  // TODO: Replace with actual icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, createNotification(content))
    }

    companion object {
        const val ACTION_CONNECT = "com.synapseguard.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.synapseguard.vpn.ACTION_DISCONNECT"

        const val EXTRA_SERVER_ADDRESS = "server_address"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_KILL_SWITCH = "kill_switch"
        const val EXTRA_SPLIT_TUNNELING = "split_tunneling"
        const val EXTRA_EXCLUDED_APPS = "excluded_apps"
        const val EXTRA_DNS_SERVERS = "dns_servers"

        private const val CHANNEL_ID = "vpn_service_channel"
        private const val NOTIFICATION_ID = 1

        // Singleton instance for state access
        @Volatile
        private var instance: VpnConnectionService? = null

        fun getInstance(): VpnConnectionService? = instance
    }

    init {
        instance = this
    }
}

enum class ConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}

data class ServiceConnectionStats(
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
}
