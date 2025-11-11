package com.synapseguard.vpn.domain.repository

import com.synapseguard.vpn.domain.model.SubscriptionTier
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user subscription status
 */
interface SubscriptionRepository {
    /**
     * Flow that emits the current subscription tier of the user
     */
    val subscriptionTier: Flow<SubscriptionTier>

    /**
     * Check if the user has a premium subscription (BASIC, PREMIUM, or ENTERPRISE)
     */
    val isPremiumUser: Flow<Boolean>

    /**
     * Update the subscription tier for the current user
     * @param tier The new subscription tier
     */
    suspend fun setSubscriptionTier(tier: SubscriptionTier)

    /**
     * Get the current subscription tier
     */
    suspend fun getCurrentTier(): SubscriptionTier

    /**
     * Check if user can access premium features
     */
    suspend fun canAccessPremiumFeatures(): Boolean
}
