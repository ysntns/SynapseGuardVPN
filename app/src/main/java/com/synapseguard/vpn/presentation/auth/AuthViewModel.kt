package com.synapseguard.vpn.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseguard.vpn.domain.model.SubscriptionTier
import com.synapseguard.vpn.domain.model.User
import com.synapseguard.vpn.domain.repository.AuthRepository
import com.synapseguard.vpn.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val error: String? = null,
    val isLoginMode: Boolean = true, // true for login, false for register
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val isPremiumUser: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Observe current user
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isAuthenticated = user != null
                )
            }
        }

        // Observe subscription tier
        viewModelScope.launch {
            subscriptionRepository.subscriptionTier.collect { tier ->
                _uiState.value = _uiState.value.copy(
                    subscriptionTier = tier,
                    isPremiumUser = tier != SubscriptionTier.FREE
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.login(email, password)
                .onSuccess { user ->
                    Timber.d("Login successful: ${user.email}")

                    // Set subscription tier based on user data
                    subscriptionRepository.setSubscriptionTier(user.subscriptionTier)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = user
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "Login failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Login failed"
                    )
                }
        }
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.register(email, password, name)
                .onSuccess { user ->
                    Timber.d("Registration successful: ${user.email}")

                    // Set subscription tier for new user (default to FREE)
                    subscriptionRepository.setSubscriptionTier(user.subscriptionTier)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentUser = user
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "Registration failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Registration failed"
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
                .onSuccess {
                    Timber.d("Logout successful")

                    // Reset subscription tier on logout
                    subscriptionRepository.setSubscriptionTier(SubscriptionTier.FREE)

                    _uiState.value = _uiState.value.copy(
                        isAuthenticated = false,
                        currentUser = null
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "Logout failed")
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Logout failed"
                    )
                }
        }
    }

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Set subscription tier (for demo/testing purposes)
     * @param tier The subscription tier to set
     */
    fun setSubscriptionTier(tier: SubscriptionTier) {
        viewModelScope.launch {
            subscriptionRepository.setSubscriptionTier(tier)
            Timber.d("Subscription tier updated to: $tier")
        }
    }

    /**
     * Toggle between FREE and PREMIUM tiers (for demo switch)
     */
    fun togglePremiumStatus() {
        viewModelScope.launch {
            val newTier = if (_uiState.value.isPremiumUser) {
                SubscriptionTier.FREE
            } else {
                SubscriptionTier.PREMIUM
            }
            setSubscriptionTier(newTier)
        }
    }
}
