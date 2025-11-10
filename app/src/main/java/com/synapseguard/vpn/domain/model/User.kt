package com.synapseguard.vpn.domain.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val subscriptionExpiryDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SubscriptionTier {
    FREE,
    BASIC,
    PREMIUM,
    ENTERPRISE
}
