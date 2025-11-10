package com.synapseguard.vpn.service.core

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import timber.log.Timber

class VpnConnectionService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        Timber.d("VpnConnectionService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
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
        try {
            // Build VPN interface
            val builder = Builder()
                .setSession("SynapseGuard VPN")
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")

            vpnInterface = builder.establish()
            Timber.d("VPN interface established")

            // TODO: Implement protocol-specific connection logic
            // This is where WireGuard, OpenVPN, or V2Ray would connect

        } catch (e: Exception) {
            Timber.e(e, "Failed to establish VPN connection")
            disconnect()
        }
    }

    private fun disconnect() {
        try {
            vpnInterface?.close()
            vpnInterface = null
            Timber.d("VPN interface closed")
        } catch (e: Exception) {
            Timber.e(e, "Error closing VPN interface")
        }
        stopSelf()
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
        Timber.d("VpnConnectionService destroyed")
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
        Timber.d("VPN permission revoked")
    }

    companion object {
        const val ACTION_CONNECT = "com.synapseguard.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.synapseguard.vpn.ACTION_DISCONNECT"
    }
}
