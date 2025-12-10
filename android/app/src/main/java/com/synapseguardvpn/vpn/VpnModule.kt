package com.synapseguardvpn.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class VpnModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext),
    ActivityEventListener {

    companion object {
        const val NAME = "VpnModule"
        private const val VPN_REQUEST_CODE = 1001

        // Event names
        const val EVENT_STATE_CHANGED = "VpnStateChanged"
        const val EVENT_STATS_UPDATED = "VpnStatsUpdated"
    }

    private var connectPromise: Promise? = null

    init {
        reactContext.addActivityEventListener(this)
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

    @ReactMethod
    fun connect(config: ReadableMap, promise: Promise) {
        try {
            val serverAddress = config.getString("serverAddress") ?: ""
            val serverPort = config.getInt("serverPort")
            val protocol = config.getString("protocol") ?: "wireguard"

            // Start VPN service
            val intent = Intent(reactContext, VpnConnectionService::class.java).apply {
                action = VpnConnectionService.ACTION_CONNECT
                putExtra(VpnConnectionService.EXTRA_SERVER_ADDRESS, serverAddress)
                putExtra(VpnConnectionService.EXTRA_SERVER_PORT, serverPort)
                putExtra(VpnConnectionService.EXTRA_PROTOCOL, protocol)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                reactContext.startForegroundService(intent)
            } else {
                reactContext.startService(intent)
            }

            promise.resolve(true)
        } catch (e: Exception) {
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
    fun sendEvent(eventName: String, params: WritableMap?) {
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
