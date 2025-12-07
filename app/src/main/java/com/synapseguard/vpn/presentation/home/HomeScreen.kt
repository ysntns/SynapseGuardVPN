package com.synapseguard.vpn.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapseguard.vpn.R
import com.synapseguard.vpn.domain.model.VpnState
import com.synapseguard.vpn.presentation.theme.BackgroundPrimary
import com.synapseguard.vpn.presentation.theme.CyanPrimary
import com.synapseguard.vpn.presentation.theme.StatusConnected
import com.synapseguard.vpn.presentation.theme.StatusDisconnected
import com.synapseguard.vpn.presentation.theme.TextPrimary
import com.synapseguard.vpn.presentation.theme.TextSecondary

@Composable
fun HomeScreen(
    onNavigateToServers: () -> Unit,
    snackbarHostState: SnackbarHostState,
    vpnPermissionGranted: Boolean,
    onRequestVpnPermission: (onPermissionGranted: () -> Unit) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BackgroundPrimary,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section (Menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { /* TODO: Open menu */ }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                }
            }

            // Middle Section (Connection)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ConnectionButton(
                    vpnState = uiState.vpnState,
                    onClick = {
                        when (uiState.vpnState) {
                            is VpnState.Connected -> viewModel.onDisconnectClick()
                            else -> {
                                if (vpnPermissionGranted) {
                                    viewModel.onConnectClick()
                                } else {
                                    onRequestVpnPermission { viewModel.onConnectClick() }
                                }
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                ConnectionStatusText(vpnState = uiState.vpnState)
            }

            // Bottom Section (Server Info)
            ServerInfoCard(
                vpnState = uiState.vpnState,
                selectedServer = uiState.selectedServer,
                onCardClick = onNavigateToServers
            )
        }
    }
}

@Composable
private fun ConnectionButton(vpnState: VpnState, onClick: () -> Unit) {
    val isConnected = vpnState is VpnState.Connected
    val circleColor = if (isConnected) StatusConnected else StatusDisconnected

    val animatedProgress by animateFloatAsState(
        targetValue = if (vpnState is VpnState.Connecting || vpnState is VpnState.Disconnecting) 0.5f else 0f,
        label = "progressAnimation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .clickable { onClick() }
            .drawBehind {
                drawArc(
                    color = circleColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 15f)
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_synapseguard_logo), // Replace with your icon
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isConnected) "CONNECTED" else "TAP TO CONNECT",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ConnectionStatusText(vpnState: VpnState) {
    val text = when (vpnState) {
        is VpnState.Connected -> "Securely Connected"
        is VpnState.Connecting -> "Connecting..."
        is VpnState.Disconnecting -> "Disconnecting..."
        is VpnState.Error -> "Connection Failed"
        is VpnState.Idle -> "Disconnected"
    }
    val color = when (vpnState) {
        is VpnState.Connected -> StatusConnected
        is VpnState.Error -> StatusDisconnected
        else -> TextSecondary
    }

    Text(
        text = text,
        color = color,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun ServerInfoCard(
    vpnState: VpnState,
    selectedServer: com.synapseguard.vpn.domain.model.VpnServer?,
    onCardClick: () -> Unit
) {
    val isConnected = vpnState is VpnState.Connected

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Flag
            val flagPainter: Painter = if (selectedServer != null) {
                painterResource(id = getFlagForCountry(selectedServer.country))
            } else {
                painterResource(id = R.drawable.ic_launcher_foreground) // Default
            }

            Image(
                painter = flagPainter,
                contentDescription = "Country Flag",
                modifier = Modifier.size(40.dp)
            )

            // Server Name & Location
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedServer?.name ?: "Select a Server",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isConnected) "Connected" else (selectedServer?.city ?: "Tap to choose"),
                    color = if (isConnected) StatusConnected else TextSecondary,
                    fontSize = 14.sp
                )
            }

            // Ping / Connection Indicator
            if (isConnected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(StatusConnected, CircleShape)
                )
            } else {
                Text(
                    text = "${selectedServer?.latency ?: "--"} ms",
                    color = CyanPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// A utility function to get drawable resource for country flag
// This needs to be implemented properly with your assets
@Composable
private fun getFlagForCountry(country: String): Int {
    return when (country.lowercase()) {
        "germany" -> R.drawable.ic_launcher_foreground // Replace with actual flag
        "france" -> R.drawable.ic_launcher_foreground
        "united states" -> R.drawable.ic_launcher_foreground
        "canada" -> R.drawable.ic_launcher_foreground
        "united kingdom" -> R.drawable.ic_launcher_foreground
        // Add more countries
        else -> R.drawable.ic_launcher_foreground
    }
}
