package com.synapseguard.vpn.service.crypto

import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WireGuard Noise Protocol Implementation (Noise_IKpsk2)
 *
 * Implements the complete Noise IK pattern with PSK as used by WireGuard:
 * - IK pattern: static-static key agreement with known initiator key
 * - psk2: Pre-shared key mixed after handshake messages
 *
 * Protocol flow:
 * 1. Initiator -> Responder: Handshake Initiation
 * 2. Responder -> Initiator: Handshake Response
 * 3. Both derive transport keys for data encryption
 *
 * Based on: https://www.wireguard.com/papers/wireguard.pdf
 */
class NoiseProtocol(private val crypto: WireGuardCrypto) {

    companion object {
        // Handshake message sizes
        const val HANDSHAKE_INITIATION_SIZE = 148
        const val HANDSHAKE_RESPONSE_SIZE = 92
        const val COOKIE_REPLY_SIZE = 64

        // Transport header size
        const val TRANSPORT_HEADER_SIZE = 16

        // Key rotation limits
        const val REJECT_AFTER_MESSAGES = 2L.shl(60) - 1
        const val REKEY_AFTER_MESSAGES = 2L.shl(60) - 2L.shl(16)
        const val REKEY_AFTER_TIME_MS = 120_000L // 2 minutes
        const val REJECT_AFTER_TIME_MS = 180_000L // 3 minutes
        const val REKEY_TIMEOUT_MS = 5_000L

        // Handshake retry limits
        const val REKEY_ATTEMPT_TIME_MS = 90_000L
        const val MAX_TIMER_HANDSHAKES = 3
    }

    /**
     * Handshake state for tracking progress
     */
    data class HandshakeState(
        var hash: ByteArray = ByteArray(32),
        var chainingKey: ByteArray = ByteArray(32),
        var ephemeralPrivate: ByteArray? = null,
        var ephemeralPublic: ByteArray? = null,
        var remoteEphemeralPublic: ByteArray? = null,
        var localStaticPrivate: ByteArray = ByteArray(32),
        var localStaticPublic: ByteArray = ByteArray(32),
        var remoteStaticPublic: ByteArray = ByteArray(32),
        var preSharedKey: ByteArray = ByteArray(32),
        var localIndex: Int = 0,
        var remoteIndex: Int = 0,
        var cookie: ByteArray? = null,
        var lastSentMac1: ByteArray? = null
    ) {
        fun destroy() {
            hash.fill(0)
            chainingKey.fill(0)
            ephemeralPrivate?.fill(0)
            ephemeralPublic?.fill(0)
            remoteEphemeralPublic?.fill(0)
            localStaticPrivate.fill(0)
            preSharedKey.fill(0)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as HandshakeState
            return localIndex == other.localIndex && remoteIndex == other.remoteIndex
        }

        override fun hashCode(): Int {
            return 31 * localIndex + remoteIndex
        }
    }

    /**
     * Creates a handshake initiation message
     *
     * Message format:
     * - msg.message_type := 1 (1 byte)
     * - msg.reserved_zero := { 0, 0, 0 } (3 bytes)
     * - msg.sender := little_endian(initiator.local_index) (4 bytes)
     * - msg.ephemeral := DH_PUBKEY(initiator.ephemeral_private) (32 bytes)
     * - msg.static := AEAD(initiator.static_public) (32 + 16 = 48 bytes)
     * - msg.timestamp := AEAD(TAI64N) (12 + 16 = 28 bytes)
     * - msg.mac1 := MAC(HASH(LABEL_MAC1 || responder.static_public), msg[0:116]) (16 bytes)
     * - msg.mac2 := MAC(initiator.last_received_cookie, msg[0:132]) (16 bytes)
     *
     * @param state Handshake state
     * @return Handshake initiation message (148 bytes)
     */
    fun createHandshakeInitiation(state: HandshakeState): ByteArray {
        Timber.d("NoiseProtocol: Creating handshake initiation")

        // Initialize protocol state
        state.chainingKey = crypto.computeInitialChainingKey()
        state.hash = crypto.computeInitialHash()

        // Mix in responder's static public key
        state.hash = crypto.mixHash(state.hash, state.remoteStaticPublic)

        // Generate ephemeral key pair
        val ephemeral = crypto.generateKeyPair()
        state.ephemeralPrivate = ephemeral.privateKey
        state.ephemeralPublic = ephemeral.publicKey

        // Generate random local index
        state.localIndex = crypto.generateRandomBytes(4).let {
            ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).int
        }

        // Build message
        val message = ByteBuffer.allocate(HANDSHAKE_INITIATION_SIZE)
        message.order(ByteOrder.LITTLE_ENDIAN)

        // Type and reserved
        message.put(WireGuardCrypto.MESSAGE_HANDSHAKE_INITIATION)
        message.put(ByteArray(3)) // reserved zeros

        // Sender index
        message.putInt(state.localIndex)

        // Ephemeral public key (unencrypted)
        message.put(state.ephemeralPublic!!)

        // C := Kdf1(C, E_pub)
        state.chainingKey = crypto.hkdf(state.chainingKey, state.ephemeralPublic!!, 1)[0]

        // H := Hash(H || E_pub)
        state.hash = crypto.mixHash(state.hash, state.ephemeralPublic!!)

        // C, k := Kdf2(C, DH(E_priv, S_pub))
        val es = crypto.x25519(state.ephemeralPrivate!!, state.remoteStaticPublic)
        val (newChainingKey1, tempKey1) = crypto.hkdf(state.chainingKey, es, 2).let {
            it[0] to it[1]
        }
        state.chainingKey = newChainingKey1

        // Encrypt static public key
        // msg.static := AEAD(k, 0, S_pub, H)
        val encryptedStatic = crypto.chacha20Poly1305Encrypt(
            tempKey1, 0, state.localStaticPublic, state.hash
        )
        message.put(encryptedStatic)

        // H := Hash(H || msg.static)
        state.hash = crypto.mixHash(state.hash, encryptedStatic)

        // C, k := Kdf2(C, DH(S_priv, S_pub))
        val ss = crypto.x25519(state.localStaticPrivate, state.remoteStaticPublic)
        val (newChainingKey2, tempKey2) = crypto.hkdf(state.chainingKey, ss, 2).let {
            it[0] to it[1]
        }
        state.chainingKey = newChainingKey2

        // Encrypt timestamp
        // msg.timestamp := AEAD(k, 0, TAI64N, H)
        val timestamp = crypto.tai64nTimestamp()
        val encryptedTimestamp = crypto.chacha20Poly1305Encrypt(
            tempKey2, 0, timestamp, state.hash
        )
        message.put(encryptedTimestamp)

        // H := Hash(H || msg.timestamp)
        state.hash = crypto.mixHash(state.hash, encryptedTimestamp)

        // Calculate MACs
        val messageWithoutMacs = message.array().copyOf(116)
        val mac1 = crypto.computeMac1(state.remoteStaticPublic, messageWithoutMacs)
        state.lastSentMac1 = mac1
        message.put(mac1)

        val messageWithMac1 = message.array().copyOf(132)
        val mac2 = crypto.computeMac2(state.cookie, messageWithMac1)
        message.put(mac2)

        Timber.d("NoiseProtocol: Handshake initiation created, sender_index=${state.localIndex}")
        return message.array()
    }

    /**
     * Processes a handshake response message
     *
     * @param state Handshake state from initiation
     * @param response Response message bytes (92 bytes)
     * @return Session keys if successful, null otherwise
     */
    fun processHandshakeResponse(
        state: HandshakeState,
        response: ByteArray
    ): WireGuardSessionKeys? {
        if (response.size != HANDSHAKE_RESPONSE_SIZE) {
            Timber.e("NoiseProtocol: Invalid response size: ${response.size}")
            return null
        }

        val buffer = ByteBuffer.wrap(response).order(ByteOrder.LITTLE_ENDIAN)

        // Verify message type
        val messageType = buffer.get()
        if (messageType != WireGuardCrypto.MESSAGE_HANDSHAKE_RESPONSE) {
            Timber.e("NoiseProtocol: Invalid message type: $messageType")
            return null
        }

        // Skip reserved bytes
        buffer.position(buffer.position() + 3)

        // Read sender index (responder's index)
        state.remoteIndex = buffer.int

        // Read receiver index (should match our local index)
        val receiverIndex = buffer.int
        if (receiverIndex != state.localIndex) {
            Timber.e("NoiseProtocol: Index mismatch: expected ${state.localIndex}, got $receiverIndex")
            return null
        }

        // Read responder's ephemeral public key
        state.remoteEphemeralPublic = ByteArray(32)
        buffer.get(state.remoteEphemeralPublic!!)

        // C := Kdf1(C, E_pub)
        state.chainingKey = crypto.hkdf(state.chainingKey, state.remoteEphemeralPublic!!, 1)[0]

        // H := Hash(H || E_pub)
        state.hash = crypto.mixHash(state.hash, state.remoteEphemeralPublic!!)

        // C, k := Kdf2(C, DH(E_priv, E_pub))
        val ee = crypto.x25519(state.ephemeralPrivate!!, state.remoteEphemeralPublic!!)
        val (newChainingKey1, tempKey1) = crypto.hkdf(state.chainingKey, ee, 2).let {
            it[0] to it[1]
        }
        state.chainingKey = newChainingKey1

        // C, k := Kdf2(C, DH(S_priv, E_pub))
        val se = crypto.x25519(state.localStaticPrivate, state.remoteEphemeralPublic!!)
        val (newChainingKey2, tempKey2) = crypto.hkdf(state.chainingKey, se, 2).let {
            it[0] to it[1]
        }
        state.chainingKey = newChainingKey2

        // Mix in pre-shared key (psk2)
        // C, t, k := Kdf3(C, psk)
        val (newChainingKey3, tempHash, tempKey3) = crypto.hkdf(
            state.chainingKey, state.preSharedKey, 3
        ).let { Triple(it[0], it[1], it[2]) }
        state.chainingKey = newChainingKey3

        // H := Hash(H || t)
        state.hash = crypto.mixHash(state.hash, tempHash)

        // Read and decrypt empty payload
        val encryptedEmpty = ByteArray(16) // Just the auth tag for empty payload
        buffer.get(encryptedEmpty)

        val decrypted = crypto.chacha20Poly1305Decrypt(
            tempKey3, 0, encryptedEmpty, state.hash
        )

        if (decrypted == null) {
            Timber.e("NoiseProtocol: Failed to decrypt response payload")
            return null
        }

        // H := Hash(H || msg.empty)
        state.hash = crypto.mixHash(state.hash, encryptedEmpty)

        // Derive transport keys
        // T_send, T_recv := Kdf2(C, empty)
        val (sendingKey, receivingKey) = crypto.hkdf(state.chainingKey, ByteArray(0), 2).let {
            it[0] to it[1]
        }

        Timber.d("NoiseProtocol: Handshake response processed successfully")
        Timber.d("NoiseProtocol: Session established - local_index=${state.localIndex}, remote_index=${state.remoteIndex}")

        return WireGuardSessionKeys(
            sendingKey = sendingKey,
            receivingKey = receivingKey,
            localIndex = state.localIndex,
            remoteIndex = state.remoteIndex
        )
    }

    /**
     * Creates a handshake response message (for server-side implementation)
     *
     * @param state Handshake state
     * @param initiation Received initiation message
     * @return Handshake response message (92 bytes) or null if initiation is invalid
     */
    fun createHandshakeResponse(
        state: HandshakeState,
        initiation: ByteArray
    ): Pair<ByteArray, WireGuardSessionKeys>? {
        if (initiation.size != HANDSHAKE_INITIATION_SIZE) {
            Timber.e("NoiseProtocol: Invalid initiation size")
            return null
        }

        val buffer = ByteBuffer.wrap(initiation).order(ByteOrder.LITTLE_ENDIAN)

        // Verify message type
        if (buffer.get() != WireGuardCrypto.MESSAGE_HANDSHAKE_INITIATION) {
            Timber.e("NoiseProtocol: Invalid message type")
            return null
        }

        // Skip reserved
        buffer.position(buffer.position() + 3)

        // Read sender index
        state.remoteIndex = buffer.int

        // Generate our local index
        state.localIndex = crypto.generateRandomBytes(4).let {
            ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).int
        }

        // Initialize protocol state
        state.chainingKey = crypto.computeInitialChainingKey()
        state.hash = crypto.computeInitialHash()

        // H := Hash(H || S_pub) - our static public
        state.hash = crypto.mixHash(state.hash, state.localStaticPublic)

        // Read initiator's ephemeral public
        state.remoteEphemeralPublic = ByteArray(32)
        buffer.get(state.remoteEphemeralPublic!!)

        // C := Kdf1(C, E_pub)
        state.chainingKey = crypto.hkdf(state.chainingKey, state.remoteEphemeralPublic!!, 1)[0]

        // H := Hash(H || E_pub)
        state.hash = crypto.mixHash(state.hash, state.remoteEphemeralPublic!!)

        // C, k := Kdf2(C, DH(S_priv, E_pub))
        val es = crypto.x25519(state.localStaticPrivate, state.remoteEphemeralPublic!!)
        val (ck1, tk1) = crypto.hkdf(state.chainingKey, es, 2).let { it[0] to it[1] }
        state.chainingKey = ck1

        // Decrypt static public key
        val encryptedStatic = ByteArray(48)
        buffer.get(encryptedStatic)

        val remoteStatic = crypto.chacha20Poly1305Decrypt(tk1, 0, encryptedStatic, state.hash)
        if (remoteStatic == null) {
            Timber.e("NoiseProtocol: Failed to decrypt initiator static key")
            return null
        }
        state.remoteStaticPublic = remoteStatic

        // H := Hash(H || msg.static)
        state.hash = crypto.mixHash(state.hash, encryptedStatic)

        // C, k := Kdf2(C, DH(S_priv, S_pub))
        val ss = crypto.x25519(state.localStaticPrivate, state.remoteStaticPublic)
        val (ck2, tk2) = crypto.hkdf(state.chainingKey, ss, 2).let { it[0] to it[1] }
        state.chainingKey = ck2

        // Decrypt timestamp
        val encryptedTimestamp = ByteArray(28)
        buffer.get(encryptedTimestamp)

        val timestamp = crypto.chacha20Poly1305Decrypt(tk2, 0, encryptedTimestamp, state.hash)
        if (timestamp == null) {
            Timber.e("NoiseProtocol: Failed to decrypt timestamp")
            return null
        }

        // H := Hash(H || msg.timestamp)
        state.hash = crypto.mixHash(state.hash, encryptedTimestamp)

        // Generate our ephemeral key pair
        val ephemeral = crypto.generateKeyPair()
        state.ephemeralPrivate = ephemeral.privateKey
        state.ephemeralPublic = ephemeral.publicKey

        // Build response
        val response = ByteBuffer.allocate(HANDSHAKE_RESPONSE_SIZE)
        response.order(ByteOrder.LITTLE_ENDIAN)

        // Message type and reserved
        response.put(WireGuardCrypto.MESSAGE_HANDSHAKE_RESPONSE)
        response.put(ByteArray(3))

        // Sender index (our index)
        response.putInt(state.localIndex)

        // Receiver index (their index)
        response.putInt(state.remoteIndex)

        // Ephemeral public key
        response.put(state.ephemeralPublic!!)

        // C := Kdf1(C, E_pub)
        state.chainingKey = crypto.hkdf(state.chainingKey, state.ephemeralPublic!!, 1)[0]

        // H := Hash(H || E_pub)
        state.hash = crypto.mixHash(state.hash, state.ephemeralPublic!!)

        // C, k := Kdf2(C, DH(E_priv, E_pub))
        val ee = crypto.x25519(state.ephemeralPrivate!!, state.remoteEphemeralPublic!!)
        val (ck3, _) = crypto.hkdf(state.chainingKey, ee, 2).let { it[0] to it[1] }
        state.chainingKey = ck3

        // C, k := Kdf2(C, DH(E_priv, S_pub))
        val se = crypto.x25519(state.ephemeralPrivate!!, state.remoteStaticPublic)
        val (ck4, _) = crypto.hkdf(state.chainingKey, se, 2).let { it[0] to it[1] }
        state.chainingKey = ck4

        // Mix PSK
        val (ck5, tempHash, tk5) = crypto.hkdf(state.chainingKey, state.preSharedKey, 3)
            .let { Triple(it[0], it[1], it[2]) }
        state.chainingKey = ck5

        // H := Hash(H || t)
        state.hash = crypto.mixHash(state.hash, tempHash)

        // Encrypt empty payload
        val encryptedEmpty = crypto.chacha20Poly1305Encrypt(tk5, 0, ByteArray(0), state.hash)
        response.put(encryptedEmpty)

        // H := Hash(H || msg.empty)
        state.hash = crypto.mixHash(state.hash, encryptedEmpty)

        // MACs
        val mac1 = crypto.computeMac1(state.remoteStaticPublic, response.array().copyOf(60))
        response.put(mac1)

        val mac2 = crypto.computeMac2(state.cookie, response.array().copyOf(76))
        response.put(mac2)

        // Derive transport keys (swapped for responder)
        val (recvKey, sendKey) = crypto.hkdf(state.chainingKey, ByteArray(0), 2).let {
            it[0] to it[1]
        }

        val sessionKeys = WireGuardSessionKeys(
            sendingKey = sendKey,
            receivingKey = recvKey,
            localIndex = state.localIndex,
            remoteIndex = state.remoteIndex
        )

        Timber.d("NoiseProtocol: Handshake response created")
        return response.array() to sessionKeys
    }

    /**
     * Encrypts a transport data packet
     *
     * @param sessionKeys Current session keys
     * @param plaintext Data to encrypt
     * @param counter Sending counter
     * @return Encrypted packet with header
     */
    fun encryptTransportData(
        sessionKeys: WireGuardSessionKeys,
        plaintext: ByteArray,
        counter: Long
    ): ByteArray {
        val header = ByteBuffer.allocate(TRANSPORT_HEADER_SIZE)
        header.order(ByteOrder.LITTLE_ENDIAN)

        // Message type
        header.put(WireGuardCrypto.MESSAGE_TRANSPORT_DATA)
        header.put(ByteArray(3)) // reserved

        // Receiver index
        header.putInt(sessionKeys.remoteIndex)

        // Counter
        header.putLong(counter)

        // Encrypt payload
        val ciphertext = crypto.chacha20Poly1305Encrypt(
            sessionKeys.sendingKey,
            counter,
            plaintext,
            ByteArray(0) // No AAD for transport data
        )

        return header.array() + ciphertext
    }

    /**
     * Decrypts a transport data packet
     *
     * @param sessionKeys Current session keys
     * @param packet Encrypted packet with header
     * @return Decrypted plaintext or null if authentication fails
     */
    fun decryptTransportData(
        sessionKeys: WireGuardSessionKeys,
        packet: ByteArray
    ): ByteArray? {
        if (packet.size < TRANSPORT_HEADER_SIZE + 16) { // Minimum: header + auth tag
            Timber.e("NoiseProtocol: Packet too small")
            return null
        }

        val header = ByteBuffer.wrap(packet, 0, TRANSPORT_HEADER_SIZE)
        header.order(ByteOrder.LITTLE_ENDIAN)

        // Verify message type
        if (header.get() != WireGuardCrypto.MESSAGE_TRANSPORT_DATA) {
            Timber.e("NoiseProtocol: Invalid message type for transport data")
            return null
        }

        // Skip reserved
        header.position(header.position() + 3)

        // Verify receiver index
        val receiverIndex = header.int
        if (receiverIndex != sessionKeys.localIndex) {
            Timber.e("NoiseProtocol: Index mismatch")
            return null
        }

        // Get counter
        val counter = header.long

        // Decrypt payload
        val ciphertext = packet.copyOfRange(TRANSPORT_HEADER_SIZE, packet.size)

        return crypto.chacha20Poly1305Decrypt(
            sessionKeys.receivingKey,
            counter,
            ciphertext,
            ByteArray(0)
        )
    }
}
