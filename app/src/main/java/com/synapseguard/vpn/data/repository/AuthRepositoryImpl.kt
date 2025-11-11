package com.synapseguard.vpn.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.synapseguard.vpn.di.IoDispatcher
import com.synapseguard.vpn.domain.model.SubscriptionTier
import com.synapseguard.vpn.domain.model.User
import com.synapseguard.vpn.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore("auth_prefs")

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    private val userIdKey = stringPreferencesKey("user_id")
    private val userEmailKey = stringPreferencesKey("user_email")
    private val userNameKey = stringPreferencesKey("user_name")
    private val subscriptionTierKey = stringPreferencesKey("subscription_tier")

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: Flow<User?> = _currentUser

    init {
        // Load current user from DataStore on init
        kotlinx.coroutines.CoroutineScope(ioDispatcher).launch {
            loadCurrentUser()
        }
    }

    private suspend fun loadCurrentUser() {
        withContext(ioDispatcher) {
            try {
                val preferences = context.authDataStore.data.first()
                val userId = preferences[userIdKey]
                val email = preferences[userEmailKey]
                val name = preferences[userNameKey]
                val tierString = preferences[subscriptionTierKey]

                if (userId != null && email != null && name != null) {
                    val user = User(
                        id = userId,
                        email = email,
                        name = name,
                        subscriptionTier = tierString?.let {
                            try {
                                SubscriptionTier.valueOf(it)
                            } catch (e: Exception) {
                                SubscriptionTier.FREE
                            }
                        } ?: SubscriptionTier.FREE
                    )
                    _currentUser.value = user
                    Timber.d("Loaded user from storage: ${user.email}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load current user")
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = withContext(ioDispatcher) {
        try {
            // Simulate API call
            delay(1000)

            // Mock authentication - in production, verify with backend
            if (email.isNotEmpty() && password.length >= 6) {
                val user = User(
                    id = UUID.randomUUID().toString(),
                    email = email,
                    name = email.substringBefore("@"),
                    subscriptionTier = if (email.contains("premium")) SubscriptionTier.PREMIUM else SubscriptionTier.FREE
                )

                // Save to DataStore
                saveUserToDataStore(user)
                _currentUser.value = user

                Timber.d("User logged in: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid email or password"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, name: String): Result<User> = withContext(ioDispatcher) {
        try {
            // Simulate API call
            delay(1500)

            // Mock registration
            if (email.isNotEmpty() && password.length >= 6 && name.isNotEmpty()) {
                val user = User(
                    id = UUID.randomUUID().toString(),
                    email = email,
                    name = name,
                    subscriptionTier = SubscriptionTier.FREE
                )

                // Save to DataStore
                saveUserToDataStore(user)
                _currentUser.value = user

                Timber.d("User registered: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("Invalid registration data"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Registration failed")
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(ioDispatcher) {
        try {
            // Clear DataStore
            context.authDataStore.edit { preferences ->
                preferences.clear()
            }
            _currentUser.value = null
            Timber.d("User logged out")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            Result.failure(e)
        }
    }

    override suspend fun isAuthenticated(): Boolean = withContext(ioDispatcher) {
        _currentUser.value != null
    }

    override suspend fun getCurrentUser(): User? = withContext(ioDispatcher) {
        _currentUser.value
    }

    private suspend fun saveUserToDataStore(user: User) {
        context.authDataStore.edit { preferences ->
            preferences[userIdKey] = user.id
            preferences[userEmailKey] = user.email
            preferences[userNameKey] = user.name
            preferences[subscriptionTierKey] = user.subscriptionTier.name
        }
    }
}
