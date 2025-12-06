package com.synapseguard.vpn.domain.usecase

import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.domain.repository.VpnRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for ConnectVpnUseCase
 *
 * Tests cover:
 * - Successful connection
 * - Connection failure handling
 * - Parameter validation
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectVpnUseCaseTest {

    @Mock
    private lateinit var vpnRepository: VpnRepository

    private lateinit var useCase: ConnectVpnUseCase

    private val testServer = VpnServer(
        id = "server1",
        name = "Germany",
        country = "Germany",
        city = "Frankfurt",
        ipAddress = "10.0.0.1",
        port = 51820,
        protocol = "WireGuard",
        load = 45,
        latency = 25,
        isFavorite = false,
        isPremium = false
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = ConnectVpnUseCase(vpnRepository)
    }

    @Test
    fun `successful connection returns success result`() = runTest {
        // Given
        whenever(vpnRepository.connect(testServer)).thenReturn(Result.success(Unit))

        // When
        val result = useCase(testServer)

        // Then
        assertTrue(result.isSuccess)
        verify(vpnRepository).connect(testServer)
    }

    @Test
    fun `connection failure returns failure result`() = runTest {
        // Given
        val exception = Exception("Connection failed")
        whenever(vpnRepository.connect(testServer)).thenReturn(Result.failure(exception))

        // When
        val result = useCase(testServer)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Connection failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `connects with correct server parameters`() = runTest {
        // Given
        whenever(vpnRepository.connect(testServer)).thenReturn(Result.success(Unit))

        // When
        useCase(testServer)

        // Then
        verify(vpnRepository).connect(testServer)
    }

    @Test
    fun `handles network timeout gracefully`() = runTest {
        // Given
        val timeoutException = java.net.SocketTimeoutException("Connection timed out")
        whenever(vpnRepository.connect(testServer)).thenReturn(Result.failure(timeoutException))

        // When
        val result = useCase(testServer)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is java.net.SocketTimeoutException)
    }

    @Test
    fun `handles security exception gracefully`() = runTest {
        // Given
        val securityException = SecurityException("VPN permission denied")
        whenever(vpnRepository.connect(testServer)).thenReturn(Result.failure(securityException))

        // When
        val result = useCase(testServer)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
