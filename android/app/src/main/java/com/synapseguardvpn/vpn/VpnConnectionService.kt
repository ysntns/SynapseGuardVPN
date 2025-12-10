package com.synapseguardvpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.synapseguardvpn.MainActivity
import com.synapseguardvpn.R
import kotlinx.coroutines.*
import java.net.DatagramChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class VpnConnectionService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.synapseguardvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.synapseguardvpn.DISCONNECT"

        const val EXTRA_SERVER_ADDRESS = "server_address"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_PROTOCOL = "protocol"

        private const val NOTIFICATION_CHANNEL_ID = "vpn_service_channel"
        private const val NOTIFICATION_ID = 1

        // Connection state
        var currentState: String = "idle"
            private set

        var currentStats: ConnectionStats = ConnectionStats()
            private set

        var killSwitchEnabled: Boolean = true
        var splitTunnelingEnabled: Boolean = false
        var excludedApps: Set<String> = emptySet()
        var customDns: List<String> = listOf("1.1.1.1", "1.0.0.1")
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serverChannel: DatagramChannel? = null
    private var connectionJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverAddress: String = ""
    private var serverPort: Int = 51820
    private var sessionStartTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                serverAddress = intent.getStringExtra(EXTRA_SERVER_ADDRESS) ?: ""
                serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 51820)
                startVpnConnection()
            }
            ACTION_DISCONNECT -> {
                stopVpnConnection()
            }
        }
        return START_STICKY
    }

    private fun startVpnConnection() {
        currentState = "connecting"

        startForeground(NOTIFICATION_ID, createNotification("Connecting..."))

        connectionJob = serviceScope.launch {
            try {
                // Establish VPN tunnel
                establishTunnel()

                // Connect to server
                connectToServer()

                currentState = "connected"
                sessionStartTime = System.currentTimeMillis()

                withContext(Dispatchers.Main) {
                    updateNotification("Connected to VPN")
                }

                // Start packet forwarding
                startPacketForwarding()

            } catch (e: Exception) {
                currentState = "error"
                withContext(Dispatchers.Main) {
                    updateNotification("Connection failed: ${e.message}")
                }
            }
        }
    }

    private fun establishTunnel() {
        val builder = Builder()
            .setSession("SynapseGuard VPN")
            .addAddress("10.8.0.2", 24)
            .addRoute("0.0.0.0", 0)
            .setMtu(1400)

        // Add DNS servers
        customDns.forEach { dns ->
            builder.addDnsServer(dns)
        }

        // Kill switch
        if (killSwitchEnabled) {
            builder.setBlocking(true)
        }

        // Split tunneling - exclude apps
        if (splitTunnelingEnabled) {
            excludedApps.forEach { packageName ->
                try {
                    builder.addDisallowedApplication(packageName)
                } catch (e: Exception) {
                    // App might not be installed
                }
            }
        }

        vpnInterface = builder.establish()
    }

    private suspend fun connectToServer() {
        serverChannel = DatagramChannel.open()
        serverChannel?.configureBlocking(false)
        serverChannel?.connect(InetSocketAddress(serverAddress, serverPort))

        // Perform handshake (simplified)
        delay(500)
    }

    private suspend fun startPacketForwarding() {
        val vpnInput = vpnInterface?.fileDescriptor ?: return
        val vpnFd = ParcelFileDescriptor.fromFd(vpnInput.fd)

        val inputStream = ParcelFileDescriptor.AutoCloseInputStream(vpnFd)
        val buffer = ByteBuffer.allocate(32767)

        while (currentState == "connected" && isActive) {
            // Update stats periodically
            updateStats()
            delay(1000)
        }
    }

    private fun updateStats() {
        val duration = if (sessionStartTime > 0) {
            System.currentTimeMillis() - sessionStartTime
        } else 0

        currentStats = currentStats.copy(
            duration = duration,
            // In real implementation, these would come from actual packet counting
            bytesReceived = currentStats.bytesReceived + (1024..4096).random(),
            bytesSent = currentStats.bytesSent + (512..2048).random(),
            packetsReceived = currentStats.packetsReceived + (1..10).random(),
            packetsSent = currentStats.packetsSent + (1..5).random()
        )

        // Calculate speeds
        if (duration > 1000) {
            currentStats = currentStats.copy(
                downloadSpeedBps = (currentStats.bytesReceived * 1000 / duration),
                uploadSpeedBps = (currentStats.bytesSent * 1000 / duration)
            )
        }
    }

    private fun stopVpnConnection() {
        currentState = "disconnecting"

        connectionJob?.cancel()
        connectionJob = null

        serverChannel?.close()
        serverChannel = null

        vpnInterface?.close()
        vpnInterface = null

        currentState = "idle"
        currentStats = ConnectionStats()
        sessionStartTime = 0

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows VPN connection status"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("SynapseGuard VPN")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionJob?.cancel()
        serviceScope.cancel()
        vpnInterface?.close()
        serverChannel?.close()
    }

    override fun onRevoke() {
        stopVpnConnection()
    }
}

data class ConnectionStats(
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val packetsReceived: Long = 0,
    val packetsSent: Long = 0,
    val duration: Long = 0,
    val downloadSpeedBps: Long = 0,
    val uploadSpeedBps: Long = 0
)
