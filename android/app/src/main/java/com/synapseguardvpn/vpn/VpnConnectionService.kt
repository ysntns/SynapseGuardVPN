package com.synapseguardvpn.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.synapseguardvpn.MainActivity
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.backend.Backend
import com.wireguard.config.Config
import kotlinx.coroutines.*

class VpnConnectionService : VpnService() {

    companion object {
        private const val TAG = "VpnConnectionService"
        const val ACTION_CONNECT = "com.synapseguardvpn.CONNECT"
        const val ACTION_DISCONNECT = "com.synapseguardvpn.DISCONNECT"

        const val EXTRA_PRIVATE_KEY = "private_key"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_DNS = "dns"
        const val EXTRA_SERVER_PUBLIC_KEY = "server_public_key"
        const val EXTRA_SERVER_ENDPOINT = "server_endpoint"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_ALLOWED_IPS = "allowed_ips"

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

        // Callback for state changes
        var onStateChanged: ((String) -> Unit)? = null
        var onStatsUpdated: ((ConnectionStats) -> Unit)? = null
    }

    private var backend: GoBackend? = null
    private var tunnel: SynapseGuardTunnel? = null
    private var currentConfig: Config? = null
    private var statsJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sessionStartTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Initialize WireGuard backend
        backend = GoBackend(this)
        Log.d(TAG, "WireGuard GoBackend initialized")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val privateKey = intent.getStringExtra(EXTRA_PRIVATE_KEY) ?: ""
                val address = intent.getStringExtra(EXTRA_ADDRESS) ?: "10.0.0.2/32"
                val dns = intent.getStringArrayExtra(EXTRA_DNS)?.toList() ?: customDns
                val serverPublicKey = intent.getStringExtra(EXTRA_SERVER_PUBLIC_KEY) ?: ""
                val serverEndpoint = intent.getStringExtra(EXTRA_SERVER_ENDPOINT) ?: ""
                val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, 51820)
                val allowedIPs = intent.getStringArrayExtra(EXTRA_ALLOWED_IPS)?.toList()
                    ?: listOf("0.0.0.0/0", "::/0")

                if (privateKey.isEmpty() || serverPublicKey.isEmpty() || serverEndpoint.isEmpty()) {
                    Log.e(TAG, "Missing required WireGuard configuration")
                    updateState("error")
                    return START_NOT_STICKY
                }

                val config = WireGuardConfiguration(
                    privateKey = privateKey,
                    address = address,
                    dns = dns,
                    serverPublicKey = serverPublicKey,
                    serverEndpoint = serverEndpoint,
                    serverPort = serverPort,
                    allowedIPs = allowedIPs
                )

                startVpnConnection(config)
            }
            ACTION_DISCONNECT -> {
                stopVpnConnection()
            }
        }
        return START_STICKY
    }

    private fun startVpnConnection(config: WireGuardConfiguration) {
        updateState("connecting")
        startForeground(NOTIFICATION_ID, createNotification("Connecting..."))

        serviceScope.launch {
            try {
                // Create WireGuard config
                currentConfig = config.toWireGuardConfig()
                Log.d(TAG, "WireGuard config created")

                // Create tunnel
                tunnel = SynapseGuardTunnel("synapseguard")

                // Start tunnel with backend
                backend?.setState(tunnel!!, Tunnel.State.UP, currentConfig)
                Log.d(TAG, "WireGuard tunnel started")

                sessionStartTime = System.currentTimeMillis()
                updateState("connected")

                withContext(Dispatchers.Main) {
                    updateNotification("Connected to VPN")
                }

                // Start stats collection
                startStatsCollection()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect: ${e.message}", e)
                updateState("error")
                withContext(Dispatchers.Main) {
                    updateNotification("Connection failed: ${e.message}")
                }
            }
        }
    }

    private fun startStatsCollection() {
        statsJob = serviceScope.launch {
            while (currentState == "connected" && isActive) {
                try {
                    // Get real statistics from WireGuard backend
                    val statistics = backend?.getStatistics(tunnel!!)

                    val duration = System.currentTimeMillis() - sessionStartTime

                    if (statistics != null) {
                        val totalRx = statistics.totalRx()
                        val totalTx = statistics.totalTx()

                        val newStats = ConnectionStats(
                            bytesReceived = totalRx,
                            bytesSent = totalTx,
                            packetsReceived = totalRx / 1400, // Approximate packet count
                            packetsSent = totalTx / 1400,
                            duration = duration,
                            downloadSpeedBps = if (duration > 0) totalRx * 1000 / duration else 0,
                            uploadSpeedBps = if (duration > 0) totalTx * 1000 / duration else 0
                        )

                        currentStats = newStats
                        onStatsUpdated?.invoke(newStats)
                    }

                    delay(1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting stats: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    private fun stopVpnConnection() {
        updateState("disconnecting")

        serviceScope.launch {
            try {
                // Stop tunnel
                tunnel?.let { t ->
                    backend?.setState(t, Tunnel.State.DOWN, currentConfig)
                }
                Log.d(TAG, "WireGuard tunnel stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping tunnel: ${e.message}")
            }

            statsJob?.cancel()
            statsJob = null
            tunnel = null
            currentConfig = null
            sessionStartTime = 0
            currentStats = ConnectionStats()

            updateState("idle")

            withContext(Dispatchers.Main) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateState(state: String) {
        currentState = state
        onStateChanged?.invoke(state)
        Log.d(TAG, "VPN state changed to: $state")
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
        serviceScope.cancel()
        statsJob?.cancel()

        try {
            tunnel?.let { t ->
                backend?.setState(t, Tunnel.State.DOWN, currentConfig)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}")
        }
    }

    override fun onRevoke() {
        stopVpnConnection()
    }
}

/**
 * Tunnel implementation for SynapseGuard
 */
class SynapseGuardTunnel(private val tunnelName: String) : Tunnel {
    override fun getName(): String = tunnelName

    override fun onStateChange(newState: Tunnel.State) {
        Log.d("SynapseGuardTunnel", "Tunnel state changed to: $newState")
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
