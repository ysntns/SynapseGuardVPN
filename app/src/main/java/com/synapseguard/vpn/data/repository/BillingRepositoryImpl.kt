package com.synapseguard.vpn.data.repository

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.SubscriptionTier
import com.synapseguard.vpn.domain.repository.BillingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BillingRepository {

    private val _availableSubscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
    override val availableSubscriptions: Flow<List<ProductDetails>> = _availableSubscriptions.asStateFlow()

    private val _activePurchases = MutableStateFlow<List<Purchase>>(emptyList())
    override val activePurchases: Flow<List<Purchase>> = _activePurchases.asStateFlow()

    private lateinit var billingClient: BillingClient
    private var currentActivity: Activity? = null

    // Create a coroutine scope for background operations
    private val billingScope = CoroutineScope(ioDispatcher + SupervisorJob())

    // Product IDs for subscriptions (these should match your Google Play Console setup)
    companion object {
        const val PRODUCT_ID_BASIC = "synapseguard_basic_monthly"
        const val PRODUCT_ID_PREMIUM = "synapseguard_premium_monthly"
        const val PRODUCT_ID_ENTERPRISE = "synapseguard_enterprise_monthly"
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { handlePurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("User canceled the purchase")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Timber.d("User already owns this item")
            }
            else -> {
                Timber.e("Purchase failed with response code: ${billingResult.responseCode}")
            }
        }
    }

    override suspend fun initialize(): Result<Unit> = withContext(ioDispatcher) {
        try {
            billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build()

            val connected = suspendCancellableCoroutine { continuation ->
                billingClient.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            Timber.d("Billing client connected successfully")
                            continuation.resume(true)
                        } else {
                            Timber.e("Billing setup failed: ${billingResult.debugMessage}")
                            continuation.resume(false)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        Timber.w("Billing service disconnected")
                        continuation.resume(false)
                    }
                })
            }

            if (connected) {
                // Query existing purchases
                queryExistingPurchases()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to connect to billing service"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing billing")
            Result.failure(e)
        }
    }

    override suspend fun querySubscriptionProducts(): Result<List<ProductDetails>> = withContext(ioDispatcher) {
        try {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID_BASIC)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID_PREMIUM)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID_ENTERPRISE)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = suspendCancellableCoroutine { continuation ->
                billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        continuation.resume(productDetailsList)
                    } else {
                        Timber.e("Failed to query products: ${billingResult.debugMessage}")
                        continuation.resume(emptyList())
                    }
                }
            }

            _availableSubscriptions.value = productDetailsResult
            Result.success(productDetailsResult)
        } catch (e: Exception) {
            Timber.e(e, "Error querying subscription products")
            Result.failure(e)
        }
    }

    override suspend fun launchPurchaseFlow(productDetails: ProductDetails): Result<Unit> {
        return try {
            val activity = currentActivity ?: return Result.failure(
                IllegalStateException("Activity not set. Call setActivity() first.")
            )

            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                ?: return Result.failure(IllegalStateException("No subscription offers available"))

            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to launch billing flow: ${billingResult.debugMessage}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error launching purchase flow")
            Result.failure(e)
        }
    }

    override suspend fun acknowledgePurchase(purchase: Purchase): Result<Unit> = withContext(ioDispatcher) {
        try {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                val ackResult = suspendCancellableCoroutine { continuation ->
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                        continuation.resume(billingResult)
                    }
                }

                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Timber.d("Purchase acknowledged successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to acknowledge purchase: ${ackResult.debugMessage}"))
                }
            } else {
                Result.success(Unit) // Already acknowledged or not in purchased state
            }
        } catch (e: Exception) {
            Timber.e(e, "Error acknowledging purchase")
            Result.failure(e)
        }
    }

    override fun getSubscriptionTierFromPurchase(purchase: Purchase): SubscriptionTier {
        return when (purchase.products.firstOrNull()) {
            PRODUCT_ID_BASIC -> SubscriptionTier.BASIC
            PRODUCT_ID_PREMIUM -> SubscriptionTier.PREMIUM
            PRODUCT_ID_ENTERPRISE -> SubscriptionTier.ENTERPRISE
            else -> SubscriptionTier.FREE
        }
    }

    override suspend fun hasActiveSubscription(): Boolean {
        return _activePurchases.value.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.isAcknowledged
        }
    }

    override fun endConnection() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
            Timber.d("Billing client connection ended")
        }
    }

    /**
     * Set the current activity for launching billing flows
     * Call this from your Activity's onCreate or onResume
     */
    fun setActivity(activity: Activity?) {
        currentActivity = activity
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Acknowledge the purchase if not already acknowledged
                billingScope.launch {
                    acknowledgePurchase(purchase)
                }
            }
        }
        _activePurchases.value = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
    }

    private suspend fun queryExistingPurchases() = withContext(ioDispatcher) {
        try {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            val purchasesResult = billingClient.queryPurchasesAsync(params)

            if (purchasesResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchasesResult.purchasesList)
                Timber.d("Queried ${purchasesResult.purchasesList.size} existing purchases")
            } else {
                Timber.e("Failed to query existing purchases: ${purchasesResult.billingResult.debugMessage}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error querying existing purchases")
        }
    }
}
