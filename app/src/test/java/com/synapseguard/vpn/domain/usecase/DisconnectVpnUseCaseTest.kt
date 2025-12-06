package com.synapseguard.vpn.domain.usecase

import com.synapseguard.vpn.domain.repository.VpnRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

/**
 * Unit tests for DisconnectVpnUseCase
 *
 * Tests cover:
 * - Successful disconnection
 * - Disconnection failure handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DisconnectVpnUseCaseTest {

    @Mock
    private lateinit var vpnRepository: VpnRepository

    private lateinit var useCase: DisconnectVpnUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = DisconnectVpnUseCase(vpnRepository)
    }

    @Test
    fun `successful disconnection returns success result`() = runTest {
        // Given
        whenever(vpnRepository.disconnect()).thenReturn(Result.success(Unit))

        // When
        val result = useCase()

        // Then
        assertTrue(result.isSuccess)
        verify(vpnRepository).disconnect()
    }

    @Test
    fun `disconnection failure returns failure result`() = runTest {
        // Given
        val exception = Exception("Disconnection failed")
        whenever(vpnRepository.disconnect()).thenReturn(Result.failure(exception))

        // When
        val result = useCase()

        // Then
        assertTrue(result.isFailure)
    }

    @Test
    fun `calls repository disconnect method`() = runTest {
        // Given
        whenever(vpnRepository.disconnect()).thenReturn(Result.success(Unit))

        // When
        useCase()

        // Then
        verify(vpnRepository).disconnect()
    }
}
