package com.synapseguard.vpn.service.wireguard

import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ReceiveWindow (anti-replay protection)
 *
 * Tests cover:
 * - Basic sequence number acceptance
 * - Duplicate detection
 * - Sliding window behavior
 * - Out-of-order packets
 * - Window reset
 */
class ReceiveWindowTest {

    private lateinit var window: ReceiveWindow

    @Before
    fun setup() {
        window = ReceiveWindow()
    }

    @Test
    fun `accepts first packet with sequence 1`() {
        assertTrue(window.check(1))
    }

    @Test
    fun `rejects sequence number 0`() {
        assertFalse(window.check(0))
    }

    @Test
    fun `rejects duplicate packet`() {
        // Given
        window.check(1)

        // Then
        assertFalse(window.check(1))
    }

    @Test
    fun `accepts sequential packets`() {
        assertTrue(window.check(1))
        assertTrue(window.check(2))
        assertTrue(window.check(3))
        assertTrue(window.check(4))
        assertTrue(window.check(5))
    }

    @Test
    fun `rejects all duplicates in sequence`() {
        window.check(1)
        window.check(2)
        window.check(3)

        assertFalse(window.check(1))
        assertFalse(window.check(2))
        assertFalse(window.check(3))
    }

    @Test
    fun `accepts out of order packets within window`() {
        assertTrue(window.check(5))
        assertTrue(window.check(3))
        assertTrue(window.check(4))
        assertTrue(window.check(1))
        assertTrue(window.check(2))
    }

    @Test
    fun `accepts packet after gap`() {
        window.check(1)
        window.check(2)
        // Gap at 3, 4
        assertTrue(window.check(5))
        // Now accept the gaps
        assertTrue(window.check(3))
        assertTrue(window.check(4))
    }

    @Test
    fun `rejects packet too old for window`() {
        // Given a window size of 2048
        val windowSize = 2048

        // Move window forward
        window.check(1)
        window.check((windowSize + 100).toLong())

        // Then packet 1 should be rejected as too old
        assertFalse(window.check(1))
    }

    @Test
    fun `accepts packet at window boundary`() {
        val windowSize = 2048

        window.check(1)
        window.check(windowSize.toLong())

        // Packet at boundary should still be valid
        assertTrue(window.check(2))
    }

    @Test
    fun `large jump clears window`() {
        // Given
        window.check(1)
        window.check(2)
        window.check(3)

        // When - large jump
        val largeSeq = 10000L
        assertTrue(window.check(largeSeq))

        // Then - old packets should be rejected
        assertFalse(window.check(1))
        assertFalse(window.check(2))
        assertFalse(window.check(3))
    }

    @Test
    fun `reset clears all state`() {
        // Given
        window.check(1)
        window.check(2)
        window.check(3)

        // When
        window.reset()

        // Then - same packets can be accepted again
        assertTrue(window.check(1))
        assertTrue(window.check(2))
        assertTrue(window.check(3))
    }

    @Test
    fun `handles sequence number near max value`() {
        val highSeq = Long.MAX_VALUE - 100

        assertTrue(window.check(highSeq))
        assertTrue(window.check(highSeq + 1))
        assertFalse(window.check(highSeq)) // Duplicate
    }

    @Test
    fun `maintains window after many packets`() {
        // Process many packets
        for (i in 1L..5000L) {
            assertTrue(window.check(i))
        }

        // Recent packets should still be tracked as seen
        assertFalse(window.check(4999))
        assertFalse(window.check(5000))

        // But very old ones are outside window
        assertFalse(window.check(1))
    }

    @Test
    fun `handles reverse order within window`() {
        // Accept packets in reverse order
        assertTrue(window.check(100))
        assertTrue(window.check(99))
        assertTrue(window.check(98))
        assertTrue(window.check(97))

        // All should be marked as received
        assertFalse(window.check(100))
        assertFalse(window.check(99))
        assertFalse(window.check(98))
        assertFalse(window.check(97))
    }

    @Test
    fun `handles interleaved packets`() {
        // Even numbers
        assertTrue(window.check(2))
        assertTrue(window.check(4))
        assertTrue(window.check(6))

        // Odd numbers
        assertTrue(window.check(1))
        assertTrue(window.check(3))
        assertTrue(window.check(5))

        // All should be marked as seen
        assertFalse(window.check(1))
        assertFalse(window.check(2))
        assertFalse(window.check(3))
        assertFalse(window.check(4))
        assertFalse(window.check(5))
        assertFalse(window.check(6))
    }

    @Test
    fun `concurrent-like access pattern`() {
        // Simulate packets arriving from multiple sources
        val sequences = listOf(1L, 5L, 2L, 8L, 3L, 6L, 4L, 7L)

        sequences.forEach { seq ->
            assertTrue(window.check(seq))
        }

        // All should be marked as received
        sequences.forEach { seq ->
            assertFalse(window.check(seq))
        }
    }
}
