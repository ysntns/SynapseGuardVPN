package com.synapseguard.vpn.domain.repository

import com.synapseguard.vpn.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, name: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun isAuthenticated(): Boolean
    suspend fun getCurrentUser(): User?
}
