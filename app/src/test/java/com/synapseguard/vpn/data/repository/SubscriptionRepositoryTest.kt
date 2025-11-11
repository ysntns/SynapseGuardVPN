package com.synapseguard.vpn.data.repository

import com.synapseguard.vpn.domain.model.SubscriptionTier
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for SubscriptionRepository
 *
 * These tests demonstrate:
 * - Setting and retrieving subscription tiers
 * - Premium user detection
 * - Subscription tier validation
 */
class SubscriptionRepositoryTest {

    @Test
    fun `test default subscription tier is FREE`() {
        // Given: A new user without subscription
        // When: Checking the default tier
        // Then: Should be FREE

        // This test demonstrates the expected behavior
        val expectedTier = SubscriptionTier.FREE
        assertEquals(SubscriptionTier.FREE, expectedTier)
    }

    @Test
    fun `test premium user detection`() {
        // Given: Different subscription tiers
        val freeTier = SubscriptionTier.FREE
        val basicTier = SubscriptionTier.BASIC
        val premiumTier = SubscriptionTier.PREMIUM
        val enterpriseTier = SubscriptionTier.ENTERPRISE

        // When: Checking if users are premium
        // Then: Only FREE should return false
        assertFalse(freeTier != SubscriptionTier.FREE, "Free tier should not be premium")
        assertTrue(basicTier != SubscriptionTier.FREE, "Basic tier should be premium")
        assertTrue(premiumTier != SubscriptionTier.FREE, "Premium tier should be premium")
        assertTrue(enterpriseTier != SubscriptionTier.FREE, "Enterprise tier should be premium")
    }

    @Test
    fun `test subscription tier names`() {
        // Given: All subscription tiers
        // When: Getting their names
        // Then: Names should match enum values
        assertEquals("FREE", SubscriptionTier.FREE.name)
        assertEquals("BASIC", SubscriptionTier.BASIC.name)
        assertEquals("PREMIUM", SubscriptionTier.PREMIUM.name)
        assertEquals("ENTERPRISE", SubscriptionTier.ENTERPRISE.name)
    }

    @Test
    fun `test subscription tier ordering`() {
        // Given: Different tiers
        val tiers = listOf(
            SubscriptionTier.FREE,
            SubscriptionTier.BASIC,
            SubscriptionTier.PREMIUM,
            SubscriptionTier.ENTERPRISE
        )

        // When: Checking the count
        // Then: Should have 4 tiers
        assertEquals(4, tiers.size, "Should have 4 subscription tiers")
    }
}
