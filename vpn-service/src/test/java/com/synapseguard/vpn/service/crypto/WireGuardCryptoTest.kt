package com.synapseguard.vpn.service.crypto

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for WireGuardCrypto
 *
 * Tests cover:
 * - Key pair generation (Curve25519)
 * - X25519 key exchange
 * - ChaCha20-Poly1305 encryption/decryption
 * - BLAKE2s hashing
 * - HKDF key derivation
 * - Replay protection
 */
class WireGuardCryptoTest {

    private lateinit var crypto: WireGuardCrypto

    @Before
    fun setup() {
        crypto = WireGuardCrypto()
    }

    // ==================== Key Generation Tests ====================

    @Test
    fun `generateKeyPair creates valid key pair`() {
        // When
        val keyPair = crypto.generateKeyPair()

        // Then
        assertNotNull(keyPair)
        assertEquals(32, keyPair.privateKey.size)
        assertEquals(32, keyPair.publicKey.size)
    }

    @Test
    fun `generateKeyPair creates unique keys each time`() {
        // When
        val keyPair1 = crypto.generateKeyPair()
        val keyPair2 = crypto.generateKeyPair()

        // Then
        assertNotEquals(keyPair1.privateKey.toList(), keyPair2.privateKey.toList())
        assertNotEquals(keyPair1.publicKey.toList(), keyPair2.publicKey.toList())
    }

    @Test
    fun `derivePublicKey produces consistent results`() {
        // Given
        val keyPair = crypto.generateKeyPair()

        // When
        val derivedPublic = crypto.derivePublicKey(keyPair.privateKey)

        // Then
        assertEquals(keyPair.publicKey.toList(), derivedPublic.toList())
    }

    // ==================== X25519 Key Exchange Tests ====================

    @Test
    fun `x25519 produces shared secret`() {
        // Given
        val aliceKeyPair = crypto.generateKeyPair()
        val bobKeyPair = crypto.generateKeyPair()

        // When
        val aliceShared = crypto.x25519(aliceKeyPair.privateKey, bobKeyPair.publicKey)
        val bobShared = crypto.x25519(bobKeyPair.privateKey, aliceKeyPair.publicKey)

        // Then
        assertEquals(aliceShared.toList(), bobShared.toList())
        assertEquals(32, aliceShared.size)
    }

    @Test
    fun `x25519 shared secret is 32 bytes`() {
        // Given
        val keyPair1 = crypto.generateKeyPair()
        val keyPair2 = crypto.generateKeyPair()

        // When
        val sharedSecret = crypto.x25519(keyPair1.privateKey, keyPair2.publicKey)

        // Then
        assertEquals(32, sharedSecret.size)
    }

    // ==================== ChaCha20-Poly1305 Tests ====================

    @Test
    fun `chacha20Poly1305 encrypt and decrypt round trip`() {
        // Given
        val key = crypto.generateRandomBytes(32)
        val plaintext = "Hello, WireGuard!".toByteArray()
        val counter = 0L

        // When
        val ciphertext = crypto.chacha20Poly1305Encrypt(key, counter, plaintext)
        val decrypted = crypto.chacha20Poly1305Decrypt(key, counter, ciphertext)

        // Then
        assertNotNull(decrypted)
        assertEquals(plaintext.toList(), decrypted.toList())
    }

    @Test
    fun `chacha20Poly1305 ciphertext is larger than plaintext`() {
        // Given
        val key = crypto.generateRandomBytes(32)
        val plaintext = "Test data".toByteArray()

        // When
        val ciphertext = crypto.chacha20Poly1305Encrypt(key, 0, plaintext)

        // Then
        assertEquals(plaintext.size + 16, ciphertext.size) // 16 bytes for auth tag
    }

    @Test
    fun `chacha20Poly1305 decryption fails with wrong key`() {
        // Given
        val key1 = crypto.generateRandomBytes(32)
        val key2 = crypto.generateRandomBytes(32)
        val plaintext = "Secret message".toByteArray()

        // When
        val ciphertext = crypto.chacha20Poly1305Encrypt(key1, 0, plaintext)
        val decrypted = crypto.chacha20Poly1305Decrypt(key2, 0, ciphertext)

        // Then
        assertEquals(null, decrypted)
    }

    @Test
    fun `chacha20Poly1305 decryption fails with tampered ciphertext`() {
        // Given
        val key = crypto.generateRandomBytes(32)
        val plaintext = "Integrity test".toByteArray()

        // When
        val ciphertext = crypto.chacha20Poly1305Encrypt(key, 0, plaintext)
        ciphertext[ciphertext.size / 2] = (ciphertext[ciphertext.size / 2].toInt() xor 0xFF).toByte()
        val decrypted = crypto.chacha20Poly1305Decrypt(key, 0, ciphertext)

        // Then
        assertEquals(null, decrypted)
    }

    @Test
    fun `chacha20Poly1305 with AAD works correctly`() {
        // Given
        val key = crypto.generateRandomBytes(32)
        val plaintext = "Protected data".toByteArray()
        val aad = "additional authenticated data".toByteArray()

        // When
        val ciphertext = crypto.chacha20Poly1305Encrypt(key, 0, plaintext, aad)
        val decrypted = crypto.chacha20Poly1305Decrypt(key, 0, ciphertext, aad)

        // Then
        assertNotNull(decrypted)
        assertEquals(plaintext.toList(), decrypted.toList())
    }

    // ==================== BLAKE2s Tests ====================

    @Test
    fun `blake2s produces 32 byte hash by default`() {
        // Given
        val data = "Test data for hashing".toByteArray()

        // When
        val hash = crypto.blake2s(data)

        // Then
        assertEquals(32, hash.size)
    }

    @Test
    fun `blake2s produces consistent hash`() {
        // Given
        val data = "Consistent data".toByteArray()

        // When
        val hash1 = crypto.blake2s(data)
        val hash2 = crypto.blake2s(data)

        // Then
        assertEquals(hash1.toList(), hash2.toList())
    }

    @Test
    fun `blake2s produces different hash for different data`() {
        // Given
        val data1 = "Data 1".toByteArray()
        val data2 = "Data 2".toByteArray()

        // When
        val hash1 = crypto.blake2s(data1)
        val hash2 = crypto.blake2s(data2)

        // Then
        assertNotEquals(hash1.toList(), hash2.toList())
    }

    @Test
    fun `blake2s with custom output length`() {
        // Given
        val data = "Test".toByteArray()

        // When
        val hash16 = crypto.blake2s(data, 16)
        val hash24 = crypto.blake2s(data, 24)

        // Then
        assertEquals(16, hash16.size)
        assertEquals(24, hash24.size)
    }

    @Test
    fun `blake2sHmac produces keyed hash`() {
        // Given
        val key = crypto.generateRandomBytes(32)
        val data = "Message to authenticate".toByteArray()

        // When
        val mac = crypto.blake2sHmac(key, data)

        // Then
        assertEquals(32, mac.size)
    }

    // ==================== HKDF Tests ====================

    @Test
    fun `hkdf produces correct number of outputs`() {
        // Given
        val chainingKey = crypto.generateRandomBytes(32)
        val ikm = crypto.generateRandomBytes(32)

        // When
        val outputs1 = crypto.hkdf(chainingKey, ikm, 1)
        val outputs2 = crypto.hkdf(chainingKey, ikm, 2)
        val outputs3 = crypto.hkdf(chainingKey, ikm, 3)

        // Then
        assertEquals(1, outputs1.size)
        assertEquals(2, outputs2.size)
        assertEquals(3, outputs3.size)
    }

    @Test
    fun `hkdf outputs are 32 bytes each`() {
        // Given
        val chainingKey = crypto.generateRandomBytes(32)
        val ikm = crypto.generateRandomBytes(32)

        // When
        val outputs = crypto.hkdf(chainingKey, ikm, 3)

        // Then
        assertTrue(outputs.all { it.size == 32 })
    }

    @Test
    fun `hkdf produces consistent outputs`() {
        // Given
        val chainingKey = crypto.generateRandomBytes(32)
        val ikm = crypto.generateRandomBytes(32)

        // When
        val outputs1 = crypto.hkdf(chainingKey, ikm, 2)
        val outputs2 = crypto.hkdf(chainingKey, ikm, 2)

        // Then
        assertEquals(outputs1[0].toList(), outputs2[0].toList())
        assertEquals(outputs1[1].toList(), outputs2[1].toList())
    }

    // ==================== Noise Protocol Helper Tests ====================

    @Test
    fun `computeInitialHash returns consistent value`() {
        // When
        val hash1 = crypto.computeInitialHash()
        val hash2 = crypto.computeInitialHash()

        // Then
        assertEquals(hash1.toList(), hash2.toList())
        assertEquals(32, hash1.size)
    }

    @Test
    fun `computeInitialChainingKey returns consistent value`() {
        // When
        val key1 = crypto.computeInitialChainingKey()
        val key2 = crypto.computeInitialChainingKey()

        // Then
        assertEquals(key1.toList(), key2.toList())
        assertEquals(32, key1.size)
    }

    @Test
    fun `mixHash combines hash and data correctly`() {
        // Given
        val hash = crypto.generateRandomBytes(32)
        val data = "Mix this data".toByteArray()

        // When
        val mixed = crypto.mixHash(hash, data)

        // Then
        assertEquals(32, mixed.size)
        assertNotEquals(hash.toList(), mixed.toList())
    }

    // ==================== TAI64N Timestamp Tests ====================

    @Test
    fun `tai64nTimestamp produces 12 byte timestamp`() {
        // When
        val timestamp = crypto.tai64nTimestamp()

        // Then
        assertEquals(12, timestamp.size)
    }

    @Test
    fun `tai64nTimestamp is monotonically increasing`() {
        // When
        val ts1 = crypto.tai64nTimestamp()
        Thread.sleep(10) // Small delay
        val ts2 = crypto.tai64nTimestamp()

        // Then - ts2 should be >= ts1 (comparing as unsigned bytes)
        val comparison = compareTimestamps(ts1, ts2)
        assertTrue(comparison <= 0)
    }

    // ==================== MAC Computation Tests ====================

    @Test
    fun `computeMac1 produces 16 byte MAC`() {
        // Given
        val peerPublicKey = crypto.generateRandomBytes(32)
        val message = "Message to authenticate".toByteArray()

        // When
        val mac1 = crypto.computeMac1(peerPublicKey, message)

        // Then
        assertEquals(16, mac1.size)
    }

    @Test
    fun `computeMac2 produces 16 byte MAC with cookie`() {
        // Given
        val cookie = crypto.generateRandomBytes(32)
        val message = "Message".toByteArray()

        // When
        val mac2 = crypto.computeMac2(cookie, message)

        // Then
        assertEquals(16, mac2.size)
    }

    @Test
    fun `computeMac2 produces zeros without cookie`() {
        // When
        val mac2 = crypto.computeMac2(null, "Message".toByteArray())

        // Then
        assertEquals(16, mac2.size)
        assertTrue(mac2.all { it == 0.toByte() })
    }

    // ==================== XOR Tests ====================

    @Test
    fun `xor produces correct result`() {
        // Given
        val a = byteArrayOf(0xFF.toByte(), 0x00, 0xAA.toByte())
        val b = byteArrayOf(0x0F, 0xF0.toByte(), 0x55)

        // When
        val result = crypto.xor(a, b)

        // Then
        assertEquals(0xF0.toByte(), result[0])
        assertEquals(0xF0.toByte(), result[1])
        assertEquals(0xFF.toByte(), result[2])
    }

    @Test
    fun `xor with itself produces zeros`() {
        // Given
        val data = crypto.generateRandomBytes(32)

        // When
        val result = crypto.xor(data, data)

        // Then
        assertTrue(result.all { it == 0.toByte() })
    }

    // ==================== Random Generation Tests ====================

    @Test
    fun `generateRandomBytes produces correct size`() {
        // When
        val random16 = crypto.generateRandomBytes(16)
        val random32 = crypto.generateRandomBytes(32)
        val random64 = crypto.generateRandomBytes(64)

        // Then
        assertEquals(16, random16.size)
        assertEquals(32, random32.size)
        assertEquals(64, random64.size)
    }

    @Test
    fun `generateRandomBytes produces unique values`() {
        // When
        val random1 = crypto.generateRandomBytes(32)
        val random2 = crypto.generateRandomBytes(32)

        // Then
        assertNotEquals(random1.toList(), random2.toList())
    }

    // ==================== Key Destruction Tests ====================

    @Test
    fun `WireGuardKeyPair destroy clears private key`() {
        // Given
        val keyPair = crypto.generateKeyPair()
        val originalPrivate = keyPair.privateKey.copyOf()

        // When
        keyPair.destroy()

        // Then
        assertTrue(keyPair.privateKey.all { it == 0.toByte() })
        assertNotEquals(originalPrivate.toList(), keyPair.privateKey.toList())
    }

    @Test
    fun `WireGuardSessionKeys destroy clears all keys`() {
        // Given
        val sessionKeys = WireGuardSessionKeys(
            sendingKey = crypto.generateRandomBytes(32),
            receivingKey = crypto.generateRandomBytes(32)
        )

        // When
        sessionKeys.destroy()

        // Then
        assertTrue(sessionKeys.sendingKey.all { it == 0.toByte() })
        assertTrue(sessionKeys.receivingKey.all { it == 0.toByte() })
    }

    // ==================== Helper Functions ====================

    private fun compareTimestamps(ts1: ByteArray, ts2: ByteArray): Int {
        for (i in ts1.indices) {
            val b1 = ts1[i].toInt() and 0xFF
            val b2 = ts2[i].toInt() and 0xFF
            if (b1 != b2) return b1 - b2
        }
        return 0
    }
}
