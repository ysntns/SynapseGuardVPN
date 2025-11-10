package com.synapseguard.vpn.domain.repository

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.synapseguard.vpn.domain.model.SubscriptionTier
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Google Play Billing operations
 */
interface BillingRepository {
    /**
     * Flow that emits the current list of available subscription products
     */
    val availableSubscriptions: Flow<List<ProductDetails>>

    /**
     * Flow that emits the list of active purchases
     */
    val activePurchases: Flow<List<Purchase>>

    /**
     * Initialize the billing client
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Query available subscription products from Google Play
     */
    suspend fun querySubscriptionProducts(): Result<List<ProductDetails>>

    /**
     * Launch the purchase flow for a subscription
     * @param productDetails The product to purchase
     */
    suspend fun launchPurchaseFlow(productDetails: ProductDetails): Result<Unit>

    /**
     * Acknowledge a purchase
     * @param purchase The purchase to acknowledge
     */
    suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit>

    /**
     * Get the subscription tier from a purchase
     * @param purchase The purchase to check
     */
    fun getSubscriptionTierFromPurchase(purchase: Purchase): SubscriptionTier

    /**
     * Check if user has an active subscription
     */
    suspend fun hasActiveSubscription(): Boolean

    /**
     * End the billing client connection
     */
    fun endConnection()
}
