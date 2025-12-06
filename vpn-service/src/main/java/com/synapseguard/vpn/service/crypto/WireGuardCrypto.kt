package com.synapseguard.vpn.service.crypto

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.engines.ChaCha20Poly1305Engine
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.crypto.digests.Blake2sDigest
import timber.log.Timber
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * WireGuard Cryptography Implementation
 *
 * Implements the complete WireGuard cryptographic primitives:
 * - Curve25519 for key exchange (X25519)
 * - ChaCha20-Poly1305 for authenticated encryption
 * - BLAKE2s for hashing and key derivation
 *
 * Based on WireGuard specification: https://www.wireguard.com/protocol/
 */
class WireGuardCrypto {

    companion object {
        // WireGuard protocol constants
        const val KEY_SIZE = 32
        const val NONCE_SIZE = 12
        const val TAG_SIZE = 16
        const val HASH_SIZE = 32

        // WireGuard construction identifiers
        private val CONSTRUCTION = "Noise_IKpsk2_25519_ChaChaPoly_BLAKE2s".toByteArray()
        private val IDENTIFIER = "WireGuard v1 zx2c4 Jason@zx2c4.com".toByteArray()
        private val LABEL_MAC1 = "mac1----".toByteArray()
        private val LABEL_COOKIE = "cookie--".toByteArray()

        // Message type constants
        const val MESSAGE_HANDSHAKE_INITIATION: Byte = 1
        const val MESSAGE_HANDSHAKE_RESPONSE: Byte = 2
        const val MESSAGE_COOKIE_REPLY: Byte = 3
        const val MESSAGE_TRANSPORT_DATA: Byte = 4
    }

    private val secureRandom = SecureRandom()

    /**
     * Generates a new Curve25519 key pair for WireGuard
     *
     * @return KeyPair containing private and public keys
     */
    fun generateKeyPair(): WireGuardKeyPair {
        val keyPairGenerator = X25519KeyPairGenerator()
        keyPairGenerator.init(X25519KeyGenerationParameters(secureRandom))
        val keyPair = keyPairGenerator.generateKeyPair()

        val privateKey = (keyPair.private as X25519PrivateKeyParameters).encoded
        val publicKey = (keyPair.public as X25519PublicKeyParameters).encoded

        Timber.d("WireGuard: Generated new key pair")
        return WireGuardKeyPair(
            privateKey = privateKey,
            publicKey = publicKey
        )
    }

    /**
     * Derives a public key from a private key using Curve25519
     *
     * @param privateKey 32-byte private key
     * @return 32-byte public key
     */
    fun derivePublicKey(privateKey: ByteArray): ByteArray {
        require(privateKey.size == KEY_SIZE) { "Private key must be $KEY_SIZE bytes" }

        val privateKeyParams = X25519PrivateKeyParameters(privateKey, 0)
        return privateKeyParams.generatePublicKey().encoded
    }

    /**
     * Performs X25519 key agreement (Curve25519 ECDH)
     *
     * @param privateKey Our private key
     * @param publicKey Peer's public key
     * @return 32-byte shared secret
     */
    fun x25519(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        require(privateKey.size == KEY_SIZE) { "Private key must be $KEY_SIZE bytes" }
        require(publicKey.size == KEY_SIZE) { "Public key must be $KEY_SIZE bytes" }

        val agreement = X25519Agreement()
        val privateParams = X25519PrivateKeyParameters(privateKey, 0)
        val publicParams = X25519PublicKeyParameters(publicKey, 0)

        agreement.init(privateParams)
        val sharedSecret = ByteArray(KEY_SIZE)
        agreement.calculateAgreement(publicParams, sharedSecret, 0)

        Timber.d("WireGuard: X25519 key agreement completed")
        return sharedSecret
    }

    /**
     * ChaCha20-Poly1305 AEAD encryption
     *
     * @param key 32-byte encryption key
     * @param counter 8-byte counter (nonce lower bits)
     * @param plaintext Data to encrypt
     * @param associatedData Additional authenticated data (AAD)
     * @return Ciphertext with 16-byte authentication tag appended
     */
    fun chacha20Poly1305Encrypt(
        key: ByteArray,
        counter: Long,
        plaintext: ByteArray,
        associatedData: ByteArray = ByteArray(0)
    ): ByteArray {
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }

        val nonce = createNonce(counter)
        val engine = ChaCha20Poly1305Engine()

        val keyParam = KeyParameter(key)
        val aeadParams = AEADParameters(keyParam, TAG_SIZE * 8, nonce, associatedData)

        engine.init(true, aeadParams)

        val ciphertext = ByteArray(engine.getOutputSize(plaintext.size))
        var len = engine.processBytes(plaintext, 0, plaintext.size, ciphertext, 0)
        len += engine.doFinal(ciphertext, len)

        return ciphertext.copyOf(len)
    }

    /**
     * ChaCha20-Poly1305 AEAD decryption
     *
     * @param key 32-byte decryption key
     * @param counter 8-byte counter (nonce lower bits)
     * @param ciphertext Encrypted data with authentication tag
     * @param associatedData Additional authenticated data (AAD)
     * @return Decrypted plaintext or null if authentication fails
     */
    fun chacha20Poly1305Decrypt(
        key: ByteArray,
        counter: Long,
        ciphertext: ByteArray,
        associatedData: ByteArray = ByteArray(0)
    ): ByteArray? {
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }

        if (ciphertext.size < TAG_SIZE) {
            Timber.e("WireGuard: Ciphertext too short for authentication tag")
            return null
        }

        val nonce = createNonce(counter)
        val engine = ChaCha20Poly1305Engine()

        val keyParam = KeyParameter(key)
        val aeadParams = AEADParameters(keyParam, TAG_SIZE * 8, nonce, associatedData)

        return try {
            engine.init(false, aeadParams)

            val plaintext = ByteArray(engine.getOutputSize(ciphertext.size))
            var len = engine.processBytes(ciphertext, 0, ciphertext.size, plaintext, 0)
            len += engine.doFinal(plaintext, len)

            plaintext.copyOf(len)
        } catch (e: Exception) {
            Timber.e(e, "WireGuard: Decryption authentication failed")
            null
        }
    }

    /**
     * BLAKE2s hash function
     *
     * @param data Data to hash
     * @param outputLength Output hash length (default 32 bytes)
     * @param key Optional key for keyed hashing
     * @return Hash output
     */
    fun blake2s(
        data: ByteArray,
        outputLength: Int = HASH_SIZE,
        key: ByteArray? = null
    ): ByteArray {
        val digest = if (key != null) {
            Blake2sDigest(key, outputLength, null, null)
        } else {
            Blake2sDigest(outputLength)
        }

        digest.update(data, 0, data.size)
        val hash = ByteArray(outputLength)
        digest.doFinal(hash, 0)

        return hash
    }

    /**
     * BLAKE2s HMAC implementation for WireGuard
     *
     * @param key HMAC key
     * @param data Data to authenticate
     * @return 32-byte MAC
     */
    fun blake2sHmac(key: ByteArray, data: ByteArray): ByteArray {
        return blake2s(data, HASH_SIZE, key)
    }

    /**
     * HKDF (HMAC-based Key Derivation Function) using BLAKE2s
     * As specified in WireGuard protocol
     *
     * @param chainingKey Current chaining key
     * @param inputKeyMaterial Input key material
     * @param numOutputs Number of 32-byte outputs (1-3)
     * @return List of derived keys
     */
    fun hkdf(
        chainingKey: ByteArray,
        inputKeyMaterial: ByteArray,
        numOutputs: Int
    ): List<ByteArray> {
        require(numOutputs in 1..3) { "Number of outputs must be 1-3" }
        require(chainingKey.size == KEY_SIZE) { "Chaining key must be $KEY_SIZE bytes" }

        // Extract
        val tempKey = blake2sHmac(chainingKey, inputKeyMaterial)

        // Expand
        val outputs = mutableListOf<ByteArray>()
        var previousOutput = ByteArray(0)

        for (i in 1..numOutputs) {
            val input = previousOutput + byteArrayOf(i.toByte())
            val output = blake2sHmac(tempKey, input)
            outputs.add(output)
            previousOutput = output
        }

        return outputs
    }

    /**
     * Computes the initial hash for WireGuard Noise protocol
     *
     * @return Initial hash value
     */
    fun computeInitialHash(): ByteArray {
        return blake2s(CONSTRUCTION + IDENTIFIER)
    }

    /**
     * Computes the initial chaining key for WireGuard Noise protocol
     *
     * @return Initial chaining key
     */
    fun computeInitialChainingKey(): ByteArray {
        return blake2s(CONSTRUCTION)
    }

    /**
     * Mixes a value into the hash
     *
     * @param hash Current hash
     * @param data Data to mix in
     * @return New hash value
     */
    fun mixHash(hash: ByteArray, data: ByteArray): ByteArray {
        return blake2s(hash + data)
    }

    /**
     * Computes MAC1 for handshake message
     *
     * @param peerPublicKey Peer's static public key
     * @param message Message to authenticate
     * @return 16-byte MAC1
     */
    fun computeMac1(peerPublicKey: ByteArray, message: ByteArray): ByteArray {
        val key = blake2s(LABEL_MAC1 + peerPublicKey)
        return blake2s(message, 16, key)
    }

    /**
     * Computes MAC2 for handshake message (cookie-based)
     *
     * @param cookie Current cookie value
     * @param message Message to authenticate
     * @return 16-byte MAC2 or zeros if no cookie
     */
    fun computeMac2(cookie: ByteArray?, message: ByteArray): ByteArray {
        return if (cookie != null && cookie.isNotEmpty()) {
            blake2s(message, 16, cookie)
        } else {
            ByteArray(16)
        }
    }

    /**
     * Generates a random 32-byte value
     *
     * @return Random bytes
     */
    fun generateRandomBytes(size: Int = KEY_SIZE): ByteArray {
        val bytes = ByteArray(size)
        secureRandom.nextBytes(bytes)
        return bytes
    }

    /**
     * Creates a 12-byte nonce from a counter value
     * WireGuard uses little-endian 64-bit counter with 4 zero bytes prefix
     */
    private fun createNonce(counter: Long): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        // First 4 bytes are zero
        // Next 8 bytes are little-endian counter
        for (i in 0..7) {
            nonce[4 + i] = (counter shr (8 * i)).toByte()
        }
        return nonce
    }

    /**
     * XOR two byte arrays
     */
    fun xor(a: ByteArray, b: ByteArray): ByteArray {
        require(a.size == b.size) { "Arrays must be same size" }
        return ByteArray(a.size) { i -> (a[i].toInt() xor b[i].toInt()).toByte() }
    }

    /**
     * Timestamp in TAI64N format
     * Used in handshake initiation for replay protection
     */
    fun tai64nTimestamp(): ByteArray {
        val seconds = System.currentTimeMillis() / 1000 + 4611686018427387914L // TAI64 epoch
        val nanos = ((System.currentTimeMillis() % 1000) * 1000000).toInt()

        val timestamp = ByteArray(12)
        // 8 bytes for seconds (big-endian)
        for (i in 0..7) {
            timestamp[i] = (seconds shr (56 - 8 * i)).toByte()
        }
        // 4 bytes for nanoseconds (big-endian)
        for (i in 0..3) {
            timestamp[8 + i] = (nanos shr (24 - 8 * i)).toByte()
        }

        return timestamp
    }
}

/**
 * WireGuard Key Pair container
 */
data class WireGuardKeyPair(
    val privateKey: ByteArray,
    val publicKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WireGuardKeyPair

        if (!privateKey.contentEquals(other.privateKey)) return false
        if (!publicKey.contentEquals(other.publicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = privateKey.contentHashCode()
        result = 31 * result + publicKey.contentHashCode()
        return result
    }

    /**
     * Securely erases the private key from memory
     */
    fun destroy() {
        privateKey.fill(0)
    }
}

/**
 * WireGuard Session Keys derived from handshake
 */
data class WireGuardSessionKeys(
    val sendingKey: ByteArray,
    val receivingKey: ByteArray,
    val sendingKeyCounter: Long = 0,
    val receivingKeyCounter: Long = 0,
    val localIndex: Int = 0,
    val remoteIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if session keys have expired (default: 2 minutes for rekey)
     */
    fun isExpired(rekeyAfterMs: Long = 120_000): Boolean {
        return System.currentTimeMillis() - createdAt > rekeyAfterMs
    }

    /**
     * Securely erases all keys from memory
     */
    fun destroy() {
        sendingKey.fill(0)
        receivingKey.fill(0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WireGuardSessionKeys

        if (!sendingKey.contentEquals(other.sendingKey)) return false
        if (!receivingKey.contentEquals(other.receivingKey)) return false
        if (localIndex != other.localIndex) return false
        if (remoteIndex != other.remoteIndex) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sendingKey.contentHashCode()
        result = 31 * result + receivingKey.contentHashCode()
        result = 31 * result + localIndex
        result = 31 * result + remoteIndex
        return result
    }
}
