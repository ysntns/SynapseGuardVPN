package com.synapseguardvpn.vpn

import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.config.InetNetwork
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import java.net.InetAddress

/**
 * WireGuard configuration data class for React Native bridge
 */
data class WireGuardConfiguration(
    val privateKey: String,
    val address: String,
    val dns: List<String>,
    val serverPublicKey: String,
    val serverEndpoint: String,
    val serverPort: Int,
    val allowedIPs: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int = 25
) {
    /**
     * Convert to WireGuard library Config object
     */
    fun toWireGuardConfig(): Config {
        val interfaceBuilder = Interface.Builder()
        interfaceBuilder.parsePrivateKey(privateKey)

        // Parse address
        address.split(",").forEach { addr ->
            interfaceBuilder.addAddress(InetNetwork.parse(addr.trim()))
        }

        // Parse DNS servers
        dns.forEach { dnsServer ->
            interfaceBuilder.addDnsServer(InetAddress.getByName(dnsServer))
        }

        val peerBuilder = Peer.Builder()
        peerBuilder.parsePublicKey(serverPublicKey)
        peerBuilder.parseEndpoint("$serverEndpoint:$serverPort")
        peerBuilder.setPersistentKeepalive(persistentKeepalive)

        // Parse allowed IPs
        allowedIPs.forEach { ip ->
            peerBuilder.addAllowedIp(InetNetwork.parse(ip))
        }

        return Config.Builder()
            .setInterface(interfaceBuilder.build())
            .addPeer(peerBuilder.build())
            .build()
    }

    companion object {
        /**
         * Generate a new WireGuard key pair
         */
        fun generateKeyPair(): Pair<String, String> {
            val keyPair = KeyPair()
            return Pair(keyPair.privateKey.toBase64(), keyPair.publicKey.toBase64())
        }

        /**
         * Derive public key from private key
         */
        fun getPublicKey(privateKey: String): String {
            val key = Key.fromBase64(privateKey)
            return KeyPair(key).publicKey.toBase64()
        }

        /**
         * Parse WireGuard config from string format
         */
        fun fromConfigString(configString: String): WireGuardConfiguration {
            val config = Config.parse(configString.byteInputStream())
            val iface = config.`interface`
            val peer = config.peers.first()

            return WireGuardConfiguration(
                privateKey = iface.keyPair.privateKey.toBase64(),
                address = iface.addresses.joinToString(",") { it.toString() },
                dns = iface.dnsServers.map { it.hostAddress ?: "" },
                serverPublicKey = peer.publicKey.toBase64(),
                serverEndpoint = peer.endpoint.get().host,
                serverPort = peer.endpoint.get().port,
                allowedIPs = peer.allowedIps.map { it.toString() },
                persistentKeepalive = peer.persistentKeepalive.orElse(25)
            )
        }
    }
}

/**
 * Server configuration with WireGuard details
 */
data class VpnServerConfig(
    val id: String,
    val name: String,
    val country: String,
    val city: String,
    val endpoint: String,
    val port: Int,
    val publicKey: String,
    val dns: List<String> = listOf("1.1.1.1", "1.0.0.1"),
    val allowedIPs: List<String> = listOf("0.0.0.0/0", "::/0")
)
