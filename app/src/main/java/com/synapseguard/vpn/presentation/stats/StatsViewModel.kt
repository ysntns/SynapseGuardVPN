package com.synapseguard.vpn.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synapseguard.vpn.domain.usecase.ObserveConnectionStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class StatsUiState(
    val downloadSpeed: Long = 0L, // bytes per second
    val uploadSpeed: Long = 0L, // bytes per second
    val totalDataUsed: Long = 0L, // bytes
    val sessionDuration: Long = 0L, // milliseconds
    val isRunningSpeedTest: Boolean = false,
    val speedTestResult: SpeedTestResult? = null,
    val dataUsageHistory: List<DataUsagePoint> = emptyList(),
    val neuralLatency: Int = 0 // milliseconds
)

data class SpeedTestResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val ping: Int
)

data class DataUsagePoint(
    val timestamp: Long,
    val bytesUsed: Long
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    observeConnectionStatsUseCase: ObserveConnectionStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        // Observe connection stats from repository
        viewModelScope.launch {
            observeConnectionStatsUseCase().collect { stats ->
                _uiState.value = _uiState.value.copy(
                    downloadSpeed = stats.bytesReceived,
                    uploadSpeed = stats.bytesSent,
                    totalDataUsed = stats.bytesReceived + stats.bytesSent,
                    sessionDuration = if (stats.bytesReceived > 0) System.currentTimeMillis() else 0
                )
            }
        }

        // Generate mock data usage history (in real app, this comes from database)
        generateMockDataHistory()

        // Simulate neural latency (BCI optimization metric)
        updateNeuralLatency()
    }

    fun runSpeedTest() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningSpeedTest = true)

            // Simulate speed test (in real app, this would be actual network test)
            kotlinx.coroutines.delay(3000)

            val result = SpeedTestResult(
                downloadMbps = Random.nextDouble(80.0, 200.0),
                uploadMbps = Random.nextDouble(40.0, 100.0),
                ping = Random.nextInt(10, 50)
            )

            _uiState.value = _uiState.value.copy(
                isRunningSpeedTest = false,
                speedTestResult = result
            )
        }
    }

    private fun generateMockDataHistory() {
        val now = System.currentTimeMillis()
        val history = List(30) { index ->
            DataUsagePoint(
                timestamp = now - (29 - index) * 24 * 60 * 60 * 1000, // Last 30 days
                bytesUsed = Random.nextLong(1_000_000_000, 5_000_000_000) // 1-5 GB per day
            )
        }
        _uiState.value = _uiState.value.copy(dataUsageHistory = history)
    }

    private fun updateNeuralLatency() {
        // BCI-optimized network latency (simulated)
        // In real implementation, this would measure actual neural processing latency
        viewModelScope.launch {
            while (true) {
                val latency = Random.nextInt(8, 18) // 8-18ms optimal for BCI
                _uiState.value = _uiState.value.copy(neuralLatency = latency)
                kotlinx.coroutines.delay(2000) // Update every 2 seconds
            }
        }
    }
}
