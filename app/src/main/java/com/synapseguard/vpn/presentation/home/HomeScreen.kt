package com.synapseguard.vpn.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.presentation.components.CircularConnectionButton
import com.synapseguard.vpn.presentation.components.ServerInfoCard
import com.synapseguard.vpn.presentation.components.StatsCard
import com.synapseguard.vpn.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    vpnPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onRequestVpnPermission: (onPermissionGranted: () -> Unit) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show snackbar when there's an error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SynapseGuard",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "BCI-Optimized Secure VPN",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Refresh server latencies button
                    IconButton(onClick = { viewModel.refreshServerLatencies() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Server Latencies",
                            tint = IconPrimary
                        )
                    }

                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "Statistics",
                            tint = IconPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundSecondary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            PermissionReminderCard(
                vpnPermissionGranted = vpnPermissionGranted,
                notificationPermissionGranted = notificationPermissionGranted,
                onRequestVpnPermission = { onRequestVpnPermission {} },
                onRequestNotificationPermission = onRequestNotificationPermission
            )

            // Circular Connection Button
            CircularConnectionButton(
                vpnState = uiState.vpnState,
                onClick = {
                    when (uiState.vpnState) {
                        is VpnState.Connected -> viewModel.onDisconnectClick()
                        is VpnState.Idle, is VpnState.Error -> if (vpnPermissionGranted) {
                            viewModel.onConnectClick()
                        } else {
                            onRequestVpnPermission { viewModel.onConnectClick() }
                        }
                        else -> {}
                    }
                },
                modifier = Modifier.padding(vertical = 24.dp)
            )

            // Status Text
            Text(
                text = getStatusText(uiState.vpnState),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = getStatusColor(uiState.vpnState)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Server Info Card
            val isConnected = uiState.vpnState is VpnState.Connected
            val server = (uiState.vpnState as? VpnState.Connected)?.server
                ?: uiState.selectedServer

            if (server != null) {
                ServerInfoCard(
                    serverName = server.name,
                    location = "${server.city}, ${server.country}",
                    pingOrIp = if (isConnected) {
                        "${server.latency}ms"
                    } else {
                        server.ipAddress
                    },
                    isConnected = isConnected
                )
            } else {
                // Default server card when no server is selected
                ServerInfoCard(
                    serverName = "No Server Selected",
                    location = "Tap to select a server",
                    pingOrIp = "--",
                    isConnected = false,
                    modifier = Modifier.clickable(onClick = onNavigateToServers)
                )
            }

            // Connection Stats (only when connected)
            if (uiState.vpnState is VpnState.Connected) {
                Spacer(modifier = Modifier.height(16.dp))

                StatsCard(
                    downloadSpeed = formatBytes(uiState.connectionStats.bytesReceived),
                    uploadSpeed = formatBytes(uiState.connectionStats.bytesSent)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

private fun getStatusText(state: VpnState): String {
    return when (state) {
        is VpnState.Idle -> "Tap to connect"
        is VpnState.Connecting -> "Establishing secure connection..."
        is VpnState.Connected -> "Your connection is secure"
        is VpnState.Disconnecting -> "Disconnecting..."
        is VpnState.Error -> state.message
    }
}

private fun getStatusColor(state: VpnState): androidx.compose.ui.graphics.Color {
    return when (state) {
        is VpnState.Idle -> TextSecondary
        is VpnState.Connecting -> StatusConnecting
        is VpnState.Connected -> StatusConnected
        is VpnState.Disconnecting -> StatusConnecting
        is VpnState.Error -> StatusDisconnected
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
        else -> "$bytes B"
    }
}

@Composable
private fun PermissionReminderCard(
    vpnPermissionGranted: Boolean,
    notificationPermissionGranted: Boolean,
    onRequestVpnPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    if (vpnPermissionGranted && notificationPermissionGranted) return

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Permissions required",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            Text(
                text = "Allow VPN access and notifications so we can keep your connection stable and alert you when the tunnel state changes.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (!vpnPermissionGranted) {
                OutlinedButton(onClick = onRequestVpnPermission) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant VPN permission", color = TextPrimary)
                }
            }

            if (!notificationPermissionGranted) {
                OutlinedButton(onClick = onRequestNotificationPermission) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable notifications", color = TextPrimary)
                }
            }
        }
    }
}
