package com.synapseguard.vpn.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.SubscriptionTier
import com.synapseguard.vpn.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_prefs")

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SubscriptionRepository {

    private companion object {
        val SUBSCRIPTION_TIER_KEY = stringPreferencesKey("subscription_tier")
    }

    override val subscriptionTier: Flow<SubscriptionTier> = context.subscriptionDataStore.data
        .map { preferences ->
            val tierName = preferences[SUBSCRIPTION_TIER_KEY] ?: SubscriptionTier.FREE.name
            try {
                SubscriptionTier.valueOf(tierName)
            } catch (e: IllegalArgumentException) {
                SubscriptionTier.FREE
            }
        }

    override val isPremiumUser: Flow<Boolean> = subscriptionTier.map { tier ->
        tier != SubscriptionTier.FREE
    }

    override suspend fun setSubscriptionTier(tier: SubscriptionTier) {
        withContext(ioDispatcher) {
            context.subscriptionDataStore.edit { preferences ->
                preferences[SUBSCRIPTION_TIER_KEY] = tier.name
            }
        }
    }

    override suspend fun getCurrentTier(): SubscriptionTier = withContext(ioDispatcher) {
        subscriptionTier.first()
    }

    override suspend fun canAccessPremiumFeatures(): Boolean = withContext(ioDispatcher) {
        val tier = getCurrentTier()
        tier != SubscriptionTier.FREE
    }
}
