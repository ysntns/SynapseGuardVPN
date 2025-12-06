package com.synapseguard.vpn.service.wireguard

import android.os.ParcelFileDescriptor
import com.synapseguard.vpn.service.core.ConnectionStats
import com.synapseguard.vpn.service.core.VpnProtocolHandler
import com.synapseguard.vpn.service.crypto.NoiseProtocol
import com.synapseguard.vpn.service.crypto.WireGuardCrypto
import com.synapseguard.vpn.service.crypto.WireGuardKeyPair
import com.synapseguard.vpn.service.crypto.WireGuardSessionKeys
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import android.util.Base64

/**
 * Production-Ready WireGuard Protocol Handler
 *
 * Implements complete WireGuard protocol with:
 * - Curve25519 key exchange (X25519)
 * - ChaCha20-Poly1305 authenticated encryption
 * - BLAKE2s hashing
 * - Noise_IKpsk2 handshake protocol
 * - Automatic key rotation
 * - Replay attack protection
 *
 * Based on WireGuard specification: https://www.wireguard.com/protocol/
 */
class WireGuardHandler : VpnProtocolHandler {

    private val crypto = WireGuardCrypto()
    private val noiseProtocol = NoiseProtocol(crypto)

    private var connected = false
    private var tunnel: WireGuardTunnel? = null
    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Connection state
    private val _connectionState = MutableStateFlow(WireGuardConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WireGuardConnectionState> = _connectionState.asStateFlow()

    // Current session keys
    private var sessionKeys: WireGuardSessionKeys? = null
    private var handshakeState: NoiseProtocol.HandshakeState? = null

    // Key pair storage
    private var localKeyPair: WireGuardKeyPair? = null
    private var peerPublicKey: ByteArray? = null
    private var preSharedKey: ByteArray = ByteArray(32) // Optional PSK

    // Counters for replay protection
    private val sendCounter = AtomicLong(0)
    private val receiveWindow = ReceiveWindow()

    // Rekey timer
    private var rekeyJob: Job? = null
    private var keepaliveJob: Job? = null

    override suspend fun connect(
        serverAddress: String,
        serverPort: Int,
        config: String,
        vpnInterface: ParcelFileDescriptor
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Timber.d("WireGuard: Initiating connection to $serverAddress:$serverPort")
            _connectionState.value = WireGuardConnectionState.CONNECTING

            // Parse configuration
            val parsedConfig = parseConfig(config)
            localKeyPair = parsedConfig.keyPair
            peerPublicKey = parsedConfig.peerPublicKey
            preSharedKey = parsedConfig.preSharedKey ?: ByteArray(32)

            Timber.d("WireGuard: Configuration parsed successfully")
            Timber.d("WireGuard: Local public key: ${Base64.encodeToString(localKeyPair!!.publicKey, Base64.NO_WRAP)}")

            // Create tunnel
            tunnel = WireGuardTunnel(
                serverAddress = serverAddress,
                serverPort = serverPort,
                vpnInterface = vpnInterface,
                crypto = crypto,
                noiseProtocol = noiseProtocol,
                localKeyPair = localKeyPair!!,
                peerPublicKey = peerPublicKey!!,
                preSharedKey = preSharedKey
            )

            // Initialize tunnel
            tunnel?.initialize()

            // Perform cryptographic handshake
            val keys = performHandshake(serverAddress, serverPort)

            if (keys == null) {
                throw SecurityException("WireGuard handshake failed - authentication error")
            }

            sessionKeys = keys
            tunnel?.setSessionKeys(keys)

            // Start tunnel operations
            tunnel?.start()

            // Start rekey timer
            startRekeyTimer()

            // Start keepalive
            startKeepalive()

            connected = true
            _connectionState.value = WireGuardConnectionState.CONNECTED
            Timber.d("WireGuard: Connected successfully with encrypted tunnel")
            Timber.d("WireGuard: Session established - local_index=${keys.localIndex}, remote_index=${keys.remoteIndex}")

            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "WireGuard: Connection failed")
            _connectionState.value = WireGuardConnectionState.ERROR
            connected = false
            tunnel?.stop()
            tunnel = null
            Result.failure(e)
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Timber.d("WireGuard: Disconnecting...")
            _connectionState.value = WireGuardConnectionState.DISCONNECTING

            // Cancel timers
            rekeyJob?.cancel()
            keepaliveJob?.cancel()

            // Stop tunnel
            tunnel?.stop()
            tunnel = null

            // Clear session keys securely
            sessionKeys?.destroy()
            sessionKeys = null

            // Clear handshake state
            handshakeState?.destroy()
            handshakeState = null

            // Reset counters
            sendCounter.set(0)
            receiveWindow.reset()

            connected = false
            _connectionState.value = WireGuardConnectionState.DISCONNECTED
            handlerScope.coroutineContext.cancelChildren()

            Timber.d("WireGuard: Disconnected successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "WireGuard: Disconnection error")
            _connectionState.value = WireGuardConnectionState.ERROR
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean = connected && sessionKeys != null

    override suspend fun getStats(): ConnectionStats {
        return tunnel?.getStats() ?: ConnectionStats()
    }

    /**
     * Performs the WireGuard Noise IK handshake
     */
    private suspend fun performHandshake(
        serverAddress: String,
        serverPort: Int
    ): WireGuardSessionKeys? = withContext(Dispatchers.IO) {
        Timber.d("WireGuard: Starting Noise IK handshake")

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = 5000 // 5 second timeout

            val serverAddr = InetSocketAddress(serverAddress, serverPort)

            // Initialize handshake state
            handshakeState = NoiseProtocol.HandshakeState().apply {
                localStaticPrivate = localKeyPair!!.privateKey
                localStaticPublic = localKeyPair!!.publicKey
                remoteStaticPublic = peerPublicKey!!
                this.preSharedKey = this@WireGuardHandler.preSharedKey
            }

            // Create handshake initiation
            val initiation = noiseProtocol.createHandshakeInitiation(handshakeState!!)
            Timber.d("WireGuard: Sending handshake initiation (${initiation.size} bytes)")

            // Send initiation
            val initiationPacket = DatagramPacket(initiation, initiation.size, serverAddr)
            socket.send(initiationPacket)

            // Wait for response
            val responseBuffer = ByteArray(NoiseProtocol.HANDSHAKE_RESPONSE_SIZE)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)

            try {
                socket.receive(responsePacket)
            } catch (e: Exception) {
                Timber.e(e, "WireGuard: Handshake response timeout")
                return@withContext null
            }

            if (responsePacket.length != NoiseProtocol.HANDSHAKE_RESPONSE_SIZE) {
                Timber.e("WireGuard: Invalid response size: ${responsePacket.length}")
                return@withContext null
            }

            Timber.d("WireGuard: Received handshake response (${responsePacket.length} bytes)")

            // Process response
            val sessionKeys = noiseProtocol.processHandshakeResponse(
                handshakeState!!,
                responsePacket.data.copyOf(responsePacket.length)
            )

            if (sessionKeys == null) {
                Timber.e("WireGuard: Handshake authentication failed")
                return@withContext null
            }

            Timber.d("WireGuard: Handshake completed successfully")
            Timber.d("WireGuard: Derived transport keys")

            return@withContext sessionKeys

        } catch (e: Exception) {
            Timber.e(e, "WireGuard: Handshake error")
            return@withContext null
        } finally {
            socket?.close()
        }
    }

    /**
     * Starts the rekey timer for automatic key rotation
     */
    private fun startRekeyTimer() {
        rekeyJob?.cancel()
        rekeyJob = handlerScope.launch {
            while (isActive && connected) {
                delay(NoiseProtocol.REKEY_AFTER_TIME_MS)

                if (!connected) break

                // Check if rekey is needed
                val keys = sessionKeys
                if (keys != null && keys.isExpired()) {
                    Timber.d("WireGuard: Session keys expired, initiating rekey")
                    performRekey()
                }

                // Check counter limits
                if (sendCounter.get() >= NoiseProtocol.REKEY_AFTER_MESSAGES) {
                    Timber.d("WireGuard: Message limit reached, initiating rekey")
                    performRekey()
                }
            }
        }
    }

    /**
     * Performs key rotation (rekey)
     */
    private suspend fun performRekey() {
        try {
            Timber.d("WireGuard: Performing rekey...")

            val tunnel = tunnel ?: return
            val serverAddress = tunnel.serverAddress
            val serverPort = tunnel.serverPort

            val newKeys = performHandshake(serverAddress, serverPort)

            if (newKeys != null) {
                // Destroy old keys
                sessionKeys?.destroy()
                sessionKeys = newKeys
                tunnel.setSessionKeys(newKeys)

                // Reset counter
                sendCounter.set(0)
                receiveWindow.reset()

                Timber.d("WireGuard: Rekey successful")
            } else {
                Timber.e("WireGuard: Rekey failed")
            }
        } catch (e: Exception) {
            Timber.e(e, "WireGuard: Rekey error")
        }
    }

    /**
     * Starts keepalive packet sending
     */
    private fun startKeepalive() {
        keepaliveJob?.cancel()
        keepaliveJob = handlerScope.launch {
            while (isActive && connected) {
                delay(25_000) // 25 seconds persistent keepalive

                if (!connected) break

                tunnel?.sendKeepalive()
            }
        }
    }

    /**
     * Parses WireGuard configuration
     */
    private fun parseConfig(config: String): WireGuardConfig {
        var privateKey: ByteArray? = null
        var peerPublicKey: ByteArray? = null
        var preSharedKey: ByteArray? = null
        var address = "10.8.0.2/24"
        val dns = mutableListOf<String>()
        var endpoint = ""
        val allowedIps = mutableListOf<String>()

        if (config.isBlank()) {
            // Generate new key pair if no config provided
            val keyPair = crypto.generateKeyPair()
            return WireGuardConfig(
                keyPair = keyPair,
                peerPublicKey = ByteArray(32), // Must be provided
                preSharedKey = null,
                address = address,
                dns = listOf("1.1.1.1", "1.0.0.1"),
                endpoint = "",
                allowedIps = listOf("0.0.0.0/0")
            )
        }

        // Parse INI-style configuration
        config.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) {
                return@forEach
            }

            val parts = trimmed.split("=", limit = 2)
            if (parts.size != 2) return@forEach

            val key = parts[0].trim()
            val value = parts[1].trim()

            when (key.lowercase()) {
                "privatekey" -> {
                    privateKey = Base64.decode(value, Base64.DEFAULT)
                }
                "publickey" -> {
                    peerPublicKey = Base64.decode(value, Base64.DEFAULT)
                }
                "presharedkey" -> {
                    preSharedKey = Base64.decode(value, Base64.DEFAULT)
                }
                "address" -> {
                    address = value
                }
                "dns" -> {
                    dns.addAll(value.split(",").map { it.trim() })
                }
                "endpoint" -> {
                    endpoint = value
                }
                "allowedips" -> {
                    allowedIps.addAll(value.split(",").map { it.trim() })
                }
            }
        }

        // Generate key pair from private key or create new
        val keyPair = if (privateKey != null) {
            val publicKey = crypto.derivePublicKey(privateKey!!)
            WireGuardKeyPair(privateKey!!, publicKey)
        } else {
            crypto.generateKeyPair()
        }

        return WireGuardConfig(
            keyPair = keyPair,
            peerPublicKey = peerPublicKey ?: ByteArray(32),
            preSharedKey = preSharedKey,
            address = address,
            dns = dns.ifEmpty { listOf("1.1.1.1", "1.0.0.1") },
            endpoint = endpoint,
            allowedIps = allowedIps.ifEmpty { listOf("0.0.0.0/0") }
        )
    }

    /**
     * Gets the next send counter and increments
     */
    fun getNextSendCounter(): Long = sendCounter.getAndIncrement()

    /**
     * Validates receive counter for replay protection
     */
    fun validateReceiveCounter(counter: Long): Boolean = receiveWindow.check(counter)
}

/**
 * WireGuard Tunnel Implementation with Real Encryption
 */
private class WireGuardTunnel(
    val serverAddress: String,
    val serverPort: Int,
    private val vpnInterface: ParcelFileDescriptor,
    private val crypto: WireGuardCrypto,
    private val noiseProtocol: NoiseProtocol,
    private val localKeyPair: WireGuardKeyPair,
    private val peerPublicKey: ByteArray,
    private val preSharedKey: ByteArray
) {
    private var running = false
    private var forwardingJob: Job? = null
    private var receiveJob: Job? = null
    private val tunnelScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Statistics
    private var bytesSent = AtomicLong(0)
    private var bytesReceived = AtomicLong(0)
    private var packetsSent = AtomicLong(0)
    private var packetsReceived = AtomicLong(0)

    // UDP socket for WireGuard communication
    private var socket: DatagramSocket? = null

    // Session keys
    private var sessionKeys: WireGuardSessionKeys? = null
    private var sendCounter = AtomicLong(0)
    private val receiveWindow = ReceiveWindow()

    fun initialize() {
        try {
            socket = DatagramSocket()
            socket?.connect(InetSocketAddress(serverAddress, serverPort))
            socket?.soTimeout = 1000

            Timber.d("WireGuard Tunnel: Initialized - connected to $serverAddress:$serverPort")
        } catch (e: Exception) {
            Timber.e(e, "WireGuard Tunnel: Failed to initialize")
            throw e
        }
    }

    fun setSessionKeys(keys: WireGuardSessionKeys) {
        // Destroy old keys
        sessionKeys?.destroy()
        sessionKeys = keys
        sendCounter.set(0)
        receiveWindow.reset()
        Timber.d("WireGuard Tunnel: Session keys updated")
    }

    fun start() {
        if (running) {
            Timber.w("WireGuard Tunnel: Already running")
            return
        }

        running = true

        // Start outbound packet forwarding (VPN -> Server)
        forwardingJob = tunnelScope.launch {
            forwardOutboundPackets()
        }

        // Start inbound packet receiving (Server -> VPN)
        receiveJob = tunnelScope.launch {
            receiveInboundPackets()
        }

        Timber.d("WireGuard Tunnel: Started with encrypted transport")
    }

    fun stop() {
        running = false
        forwardingJob?.cancel()
        receiveJob?.cancel()

        try {
            socket?.close()
        } catch (e: Exception) {
            Timber.e(e, "WireGuard Tunnel: Error closing socket")
        }

        // Securely destroy keys
        sessionKeys?.destroy()
        sessionKeys = null

        tunnelScope.coroutineContext.cancelChildren()
        Timber.d("WireGuard Tunnel: Stopped")
    }

    /**
     * Forwards outbound packets from VPN interface to WireGuard server
     */
    private suspend fun forwardOutboundPackets() {
        val inputStream = FileInputStream(vpnInterface.fileDescriptor)
        val buffer = ByteArray(65535) // Max IP packet size

        Timber.d("WireGuard Tunnel: Starting outbound packet forwarding")

        while (running) {
            try {
                val length = inputStream.read(buffer)

                if (length > 0 && sessionKeys != null) {
                    val plaintext = buffer.copyOf(length)

                    // Get counter
                    val counter = sendCounter.getAndIncrement()

                    // Check counter limits
                    if (counter >= NoiseProtocol.REJECT_AFTER_MESSAGES) {
                        Timber.w("WireGuard: Message limit exceeded, need rekey")
                        continue
                    }

                    // Encrypt packet using ChaCha20-Poly1305
                    val encryptedPacket = noiseProtocol.encryptTransportData(
                        sessionKeys!!,
                        plaintext,
                        counter
                    )

                    // Send to server
                    val packet = DatagramPacket(
                        encryptedPacket,
                        encryptedPacket.size,
                        InetSocketAddress(serverAddress, serverPort)
                    )
                    socket?.send(packet)

                    // Update stats
                    bytesSent.addAndGet(length.toLong())
                    packetsSent.incrementAndGet()
                }

            } catch (e: Exception) {
                if (running) {
                    Timber.e(e, "WireGuard Tunnel: Error forwarding outbound packet")
                    delay(10)
                }
            }
        }

        Timber.d("WireGuard Tunnel: Outbound forwarding stopped")
    }

    /**
     * Receives inbound packets from WireGuard server to VPN interface
     */
    private suspend fun receiveInboundPackets() {
        val outputStream = FileOutputStream(vpnInterface.fileDescriptor)
        val buffer = ByteArray(65535 + NoiseProtocol.TRANSPORT_HEADER_SIZE + 16)

        Timber.d("WireGuard Tunnel: Starting inbound packet receiving")

        while (running) {
            try {
                val packet = DatagramPacket(buffer, buffer.size)

                try {
                    socket?.receive(packet)
                } catch (e: java.net.SocketTimeoutException) {
                    continue
                }

                if (packet.length > 0 && sessionKeys != null) {
                    val encryptedData = packet.data.copyOf(packet.length)

                    // Check message type
                    val messageType = encryptedData[0]

                    when (messageType) {
                        WireGuardCrypto.MESSAGE_TRANSPORT_DATA -> {
                            // Extract counter for replay protection
                            val counterBytes = encryptedData.copyOfRange(8, 16)
                            val counter = ByteBuffer.wrap(counterBytes)
                                .order(ByteOrder.LITTLE_ENDIAN)
                                .long

                            // Check replay window
                            if (!receiveWindow.check(counter)) {
                                Timber.w("WireGuard: Rejected replay packet, counter=$counter")
                                continue
                            }

                            // Decrypt packet using ChaCha20-Poly1305
                            val plaintext = noiseProtocol.decryptTransportData(
                                sessionKeys!!,
                                encryptedData
                            )

                            if (plaintext != null && plaintext.isNotEmpty()) {
                                // Write decrypted packet to VPN interface
                                outputStream.write(plaintext)

                                // Update stats
                                bytesReceived.addAndGet(plaintext.size.toLong())
                                packetsReceived.incrementAndGet()
                            } else {
                                Timber.w("WireGuard: Decryption failed - authentication error")
                            }
                        }

                        WireGuardCrypto.MESSAGE_HANDSHAKE_RESPONSE -> {
                            Timber.d("WireGuard: Received handshake response")
                            // Handled separately during handshake
                        }

                        else -> {
                            Timber.w("WireGuard: Unknown message type: $messageType")
                        }
                    }
                }

            } catch (e: Exception) {
                if (running) {
                    Timber.e(e, "WireGuard Tunnel: Error receiving inbound packet")
                    delay(10)
                }
            }
        }

        Timber.d("WireGuard Tunnel: Inbound receiving stopped")
    }

    /**
     * Sends a keepalive packet (empty encrypted packet)
     */
    suspend fun sendKeepalive() {
        if (!running || sessionKeys == null) return

        try {
            val counter = sendCounter.getAndIncrement()

            // Encrypt empty packet
            val keepalive = noiseProtocol.encryptTransportData(
                sessionKeys!!,
                ByteArray(0),
                counter
            )

            val packet = DatagramPacket(
                keepalive,
                keepalive.size,
                InetSocketAddress(serverAddress, serverPort)
            )
            socket?.send(packet)

            Timber.d("WireGuard: Sent keepalive")
        } catch (e: Exception) {
            Timber.e(e, "WireGuard: Failed to send keepalive")
        }
    }

    fun getStats(): ConnectionStats {
        return ConnectionStats(
            bytesReceived = bytesReceived.get(),
            bytesSent = bytesSent.get(),
            packetsReceived = packetsReceived.get(),
            packetsSent = packetsSent.get()
        )
    }
}

/**
 * Sliding window for replay attack protection
 * Based on RFC 2401 anti-replay algorithm
 */
class ReceiveWindow(private val windowSize: Int = 2048) {
    private var highestSeq: Long = 0
    private val bitmap = LongArray(windowSize / 64 + 1)

    @Synchronized
    fun check(seq: Long): Boolean {
        if (seq == 0L) return false

        if (seq > highestSeq) {
            // New highest sequence
            val diff = seq - highestSeq
            if (diff >= windowSize) {
                // Clear entire bitmap
                bitmap.fill(0)
            } else {
                // Shift bitmap
                for (i in highestSeq + 1..seq) {
                    clearBit(i)
                }
            }
            highestSeq = seq
            setBit(seq)
            return true
        }

        val diff = highestSeq - seq
        if (diff >= windowSize) {
            // Too old
            return false
        }

        // Check if already received
        if (isBitSet(seq)) {
            return false
        }

        // Mark as received
        setBit(seq)
        return true
    }

    private fun setBit(seq: Long) {
        val index = (seq % windowSize).toInt()
        val word = index / 64
        val bit = index % 64
        bitmap[word] = bitmap[word] or (1L shl bit)
    }

    private fun clearBit(seq: Long) {
        val index = (seq % windowSize).toInt()
        val word = index / 64
        val bit = index % 64
        bitmap[word] = bitmap[word] and (1L shl bit).inv()
    }

    private fun isBitSet(seq: Long): Boolean {
        val index = (seq % windowSize).toInt()
        val word = index / 64
        val bit = index % 64
        return (bitmap[word] and (1L shl bit)) != 0L
    }

    @Synchronized
    fun reset() {
        highestSeq = 0
        bitmap.fill(0)
    }
}

/**
 * WireGuard Configuration
 */
data class WireGuardConfig(
    val keyPair: WireGuardKeyPair,
    val peerPublicKey: ByteArray,
    val preSharedKey: ByteArray?,
    val address: String = "10.8.0.2/24",
    val dns: List<String> = listOf("1.1.1.1", "1.0.0.1"),
    val endpoint: String = "",
    val allowedIps: List<String> = listOf("0.0.0.0/0")
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WireGuardConfig

        if (keyPair != other.keyPair) return false
        if (!peerPublicKey.contentEquals(other.peerPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyPair.hashCode()
        result = 31 * result + peerPublicKey.contentHashCode()
        return result
    }
}

/**
 * WireGuard Connection State
 */
enum class WireGuardConnectionState {
    DISCONNECTED,
    CONNECTING,
    HANDSHAKING,
    CONNECTED,
    DISCONNECTING,
    ERROR
}
