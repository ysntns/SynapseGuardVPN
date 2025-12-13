package com.synapseguardvpn.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.wireguard.crypto.KeyPair

class VpnModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext),
    ActivityEventListener {

    companion object {
        const val NAME = "VpnModule"
        private const val TAG = "VpnModule"
        private const val VPN_REQUEST_CODE = 1001

        // Event names
        const val EVENT_STATE_CHANGED = "VpnStateChanged"
        const val EVENT_STATS_UPDATED = "VpnStatsUpdated"
    }

    private var connectPromise: Promise? = null
    private var pendingConfig: ReadableMap? = null

    init {
        reactContext.addActivityEventListener(this)
        setupCallbacks()
    }

    private fun setupCallbacks() {
        VpnConnectionService.onStateChanged = { state ->
            val params = Arguments.createMap().apply {
                putString("state", state)
            }
            sendEvent(EVENT_STATE_CHANGED, params)
        }

        VpnConnectionService.onStatsUpdated = { stats ->
            val params = Arguments.createMap().apply {
                putDouble("bytesReceived", stats.bytesReceived.toDouble())
                putDouble("bytesSent", stats.bytesSent.toDouble())
                putDouble("packetsReceived", stats.packetsReceived.toDouble())
                putDouble("packetsSent", stats.packetsSent.toDouble())
                putDouble("duration", stats.duration.toDouble())
                putDouble("downloadSpeedBps", stats.downloadSpeedBps.toDouble())
                putDouble("uploadSpeedBps", stats.uploadSpeedBps.toDouble())
            }
            sendEvent(EVENT_STATS_UPDATED, params)
        }
    }

    override fun getName(): String = NAME

    override fun getConstants(): Map<String, Any> {
        return mapOf(
            "STATE_IDLE" to "idle",
            "STATE_CONNECTING" to "connecting",
            "STATE_CONNECTED" to "connected",
            "STATE_DISCONNECTING" to "disconnecting",
            "STATE_ERROR" to "error"
        )
    }

    @ReactMethod
    fun prepare(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("NO_ACTIVITY", "No activity available")
            return
        }

        val intent = VpnService.prepare(activity)
        if (intent != null) {
            connectPromise = promise
            activity.startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            // Already prepared
            promise.resolve(true)
        }
    }

    /**
     * Generate a new WireGuard key pair
     */
    @ReactMethod
    fun generateKeyPair(promise: Promise) {
        try {
            val keyPair = KeyPair()
            val result = Arguments.createMap().apply {
                putString("privateKey", keyPair.privateKey.toBase64())
                putString("publicKey", keyPair.publicKey.toBase64())
            }
            promise.resolve(result)
            Log.d(TAG, "Generated new key pair")
        } catch (e: Exception) {
            promise.reject("KEYGEN_ERROR", e.message, e)
        }
    }

    /**
     * Get public key from private key
     */
    @ReactMethod
    fun getPublicKey(privateKey: String, promise: Promise) {
        try {
            val publicKey = WireGuardConfiguration.getPublicKey(privateKey)
            promise.resolve(publicKey)
        } catch (e: Exception) {
            promise.reject("PUBKEY_ERROR", e.message, e)
        }
    }

    /**
     * Connect to VPN with WireGuard configuration
     */
    @ReactMethod
    fun connect(config: ReadableMap, promise: Promise) {
        try {
            // Check required fields
            val privateKey = config.getString("privateKey")
            val serverPublicKey = config.getString("serverPublicKey")
            val serverEndpoint = config.getString("serverEndpoint")

            if (privateKey.isNullOrEmpty() || serverPublicKey.isNullOrEmpty() || serverEndpoint.isNullOrEmpty()) {
                promise.reject("CONFIG_ERROR", "Missing required WireGuard configuration: privateKey, serverPublicKey, and serverEndpoint are required")
                return
            }

            val address = config.getString("address") ?: "10.0.0.2/32"
            val serverPort = if (config.hasKey("serverPort")) config.getInt("serverPort") else 51820

            // DNS servers
            val dns = if (config.hasKey("dns")) {
                val dnsArray = config.getArray("dns")
                (0 until (dnsArray?.size() ?: 0)).mapNotNull { dnsArray?.getString(it) }.toTypedArray()
            } else {
                arrayOf("1.1.1.1", "1.0.0.1")
            }

            // Allowed IPs
            val allowedIPs = if (config.hasKey("allowedIPs")) {
                val ipsArray = config.getArray("allowedIPs")
                (0 until (ipsArray?.size() ?: 0)).mapNotNull { ipsArray?.getString(it) }.toTypedArray()
            } else {
                arrayOf("0.0.0.0/0", "::/0")
            }

            Log.d(TAG, "Connecting to $serverEndpoint:$serverPort")

            // Start VPN service with WireGuard config
            val intent = Intent(reactContext, VpnConnectionService::class.java).apply {
                action = VpnConnectionService.ACTION_CONNECT
                putExtra(VpnConnectionService.EXTRA_PRIVATE_KEY, privateKey)
                putExtra(VpnConnectionService.EXTRA_ADDRESS, address)
                putExtra(VpnConnectionService.EXTRA_DNS, dns)
                putExtra(VpnConnectionService.EXTRA_SERVER_PUBLIC_KEY, serverPublicKey)
                putExtra(VpnConnectionService.EXTRA_SERVER_ENDPOINT, serverEndpoint)
                putExtra(VpnConnectionService.EXTRA_SERVER_PORT, serverPort)
                putExtra(VpnConnectionService.EXTRA_ALLOWED_IPS, allowedIPs)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                reactContext.startForegroundService(intent)
            } else {
                reactContext.startService(intent)
            }

            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Connect error: ${e.message}", e)
            promise.reject("CONNECT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun disconnect(promise: Promise) {
        try {
            val intent = Intent(reactContext, VpnConnectionService::class.java).apply {
                action = VpnConnectionService.ACTION_DISCONNECT
            }
            reactContext.startService(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("DISCONNECT_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getConnectionState(promise: Promise) {
        try {
            val state = VpnConnectionService.currentState
            promise.resolve(state)
        } catch (e: Exception) {
            promise.reject("STATE_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun getConnectionStats(promise: Promise) {
        try {
            val stats = VpnConnectionService.currentStats
            val result = Arguments.createMap().apply {
                putDouble("bytesReceived", stats.bytesReceived.toDouble())
                putDouble("bytesSent", stats.bytesSent.toDouble())
                putDouble("packetsReceived", stats.packetsReceived.toDouble())
                putDouble("packetsSent", stats.packetsSent.toDouble())
                putDouble("duration", stats.duration.toDouble())
                putDouble("downloadSpeedBps", stats.downloadSpeedBps.toDouble())
                putDouble("uploadSpeedBps", stats.uploadSpeedBps.toDouble())
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("STATS_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun setKillSwitch(enabled: Boolean, promise: Promise) {
        try {
            VpnConnectionService.killSwitchEnabled = enabled
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("KILLSWITCH_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun setSplitTunneling(enabled: Boolean, excludedApps: ReadableArray, promise: Promise) {
        try {
            VpnConnectionService.splitTunnelingEnabled = enabled
            VpnConnectionService.excludedApps = (0 until excludedApps.size())
                .mapNotNull { excludedApps.getString(it) }
                .toSet()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SPLIT_TUNNEL_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun setCustomDns(dnsServers: ReadableArray, promise: Promise) {
        try {
            VpnConnectionService.customDns = (0 until dnsServers.size())
                .mapNotNull { dnsServers.getString(it) }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("DNS_ERROR", e.message, e)
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {
        // Required for RN built-in Event Emitter
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Required for RN built-in Event Emitter
    }

    // Send events to JavaScript
    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    override fun onActivityResult(
        activity: Activity?,
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        if (requestCode == VPN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                connectPromise?.resolve(true)
            } else {
                connectPromise?.reject("VPN_DENIED", "VPN permission denied by user")
            }
            connectPromise = null
        }
    }

    override fun onNewIntent(intent: Intent?) {
        // Not used
    }
}
