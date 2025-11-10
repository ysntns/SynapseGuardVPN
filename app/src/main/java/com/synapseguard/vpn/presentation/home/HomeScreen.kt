package com.synapseguard.vpn.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
        },
        bottomBar = {
            BottomNavigationBar(
                selectedIndex = 0,
                onHomeClick = { /* Already on home */ },
                onServersClick = onNavigateToServers,
                onSettingsClick = onNavigateToSettings
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

            // Circular Connection Button
            CircularConnectionButton(
                vpnState = uiState.vpnState,
                onClick = {
                    when (uiState.vpnState) {
                        is VpnState.Connected -> viewModel.onDisconnectClick()
                        is VpnState.Idle, is VpnState.Error -> viewModel.onConnectClick()
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

            // Error message
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusDisconnected.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = StatusDisconnected,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = StatusDisconnected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedIndex: Int,
    onHomeClick: () -> Unit,
    onServersClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(
        containerColor = BackgroundSecondary,
        contentColor = TextPrimary
    ) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = onHomeClick,
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = SurfaceSelected
            )
        )

        NavigationBarItem(
            selected = selectedIndex == 1,
            onClick = onServersClick,
            icon = {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = "Servers"
                )
            },
            label = { Text("Servers") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = SurfaceSelected
            )
        )

        NavigationBarItem(
            selected = selectedIndex == 2,
            onClick = onSettingsClick,
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyanPrimary,
                selectedTextColor = CyanPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = SurfaceSelected
            )
        )
    }
}

private fun getStatusText(state: VpnState): String {
    return when (state) {
        is VpnState.Idle -> "Tap to connect"
        is VpnState.Connecting -> "Establishing secure connection..."
        is VpnState.Connected -> "Your connection is secure"
        is VpnState.Disconnecting -> "Disconnecting..."
        is VpnState.Error -> "Connection failed"
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
