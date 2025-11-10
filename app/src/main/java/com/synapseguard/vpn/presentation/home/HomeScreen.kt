package com.synapseguard.vpn.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.presentation.theme.VpnConnected
import com.synapseguard.vpn.presentation.theme.VpnConnecting
import com.synapseguard.vpn.presentation.theme.VpnDisconnected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToServers: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SynapseGuard VPN") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // VPN Status Icon
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "VPN Status",
                modifier = Modifier.size(120.dp),
                tint = when (uiState.vpnState) {
                    is VpnState.Connected -> VpnConnected
                    is VpnState.Connecting, is VpnState.Disconnecting -> VpnConnecting
                    else -> VpnDisconnected
                }
            )

            // VPN Status Text
            Text(
                text = getStatusText(uiState.vpnState),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Connection Info
            if (uiState.vpnState is VpnState.Connected) {
                val connectedState = uiState.vpnState as VpnState.Connected
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Connected to:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = connectedState.server.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${connectedState.server.city}, ${connectedState.server.country}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Connection Stats
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = "Download",
                            value = formatBytes(uiState.connectionStats.bytesReceived)
                        )
                        StatItem(
                            label = "Upload",
                            value = formatBytes(uiState.connectionStats.bytesSent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Connect/Disconnect Button
            Button(
                onClick = {
                    when (uiState.vpnState) {
                        is VpnState.Connected -> viewModel.onDisconnectClick()
                        is VpnState.Idle, is VpnState.Error -> viewModel.onConnectClick()
                        else -> {}
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (uiState.vpnState) {
                        is VpnState.Connected -> VpnDisconnected
                        else -> VpnConnected
                    }
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = when (uiState.vpnState) {
                            is VpnState.Connected -> "Disconnect"
                            else -> "Connect"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Server Selection Button
            OutlinedButton(
                onClick = onNavigateToServers,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Select Server")
            }

            // Error Snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun getStatusText(state: VpnState): String {
    return when (state) {
        is VpnState.Idle -> "Not Connected"
        is VpnState.Connecting -> "Connecting..."
        is VpnState.Connected -> "Connected"
        is VpnState.Disconnecting -> "Disconnecting..."
        is VpnState.Error -> "Connection Error"
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
