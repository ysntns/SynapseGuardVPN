package com.synapseguard.vpn.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapseguard.vpn.domain.model.VpnProtocol
import com.synapseguard.vpn.presentation.components.SettingsToggleItem
import com.synapseguard.vpn.presentation.theme.BackgroundPrimary
import com.synapseguard.vpn.presentation.theme.CyanPrimary
import com.synapseguard.vpn.presentation.theme.IconRed
import com.synapseguard.vpn.presentation.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSplitTunnel: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedProtocol by remember { mutableStateOf(uiState.settings.preferredProtocol) }

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Search */ }) {
                        Icon(Icons.Default.Info, contentDescription = "Search", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Security Features
            SectionHeader(title = "Security Features")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsToggleItem(
                title = "Kill Switch",
                icon = Icons.Default.Security,
                iconTint = IconRed,
                checked = uiState.settings.killSwitch,
                onCheckedChange = viewModel::updateKillSwitch
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsItem(
                title = "Split Tunneling",
                icon = Icons.Default.Splitscreen,
                onClick = onNavigateToSplitTunnel
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsToggleItem(
                title = "DNS Leak Protection",
                icon = Icons.Default.Dns,
                checked = true, // Always on
                onCheckedChange = {}
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Connection Options
            SectionHeader(title = "Connection Options")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsToggleItem(
                title = "Auto-Connect",
                icon = Icons.Default.PowerSettingsNew,
                checked = uiState.settings.autoConnect,
                onCheckedChange = viewModel::updateAutoConnect
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProtocolSelector(
                selectedProtocol = selectedProtocol,
                onProtocolSelected = {
                    selectedProtocol = it
                    viewModel.updatePreferredProtocol(it)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Advanced
            SectionHeader(title = "Advanced")
            Spacer(modifier = Modifier.height(8.dp))
            SettingsToggleItem(
                title = "Send Diagnostic Logs",
                icon = Icons.Default.Warning,
                checked = false, // Add to settings
                onCheckedChange = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsItem(
                title = "About SynapseGuard",
                icon = Icons.Default.Storage,
                onClick = { /* TODO */ }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
private fun SettingsItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = TextPrimary, fontSize = 16.sp)
    }
}

@Composable
private fun ProtocolSelector(
    selectedProtocol: VpnProtocol,
    onProtocolSelected: (VpnProtocol) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProtocolButton(
            text = "OpenVPN",
            isSelected = selectedProtocol == VpnProtocol.OpenVPN,
            onClick = { onProtocolSelected(VpnProtocol.OpenVPN) },
            modifier = Modifier.weight(1f)
        )
        ProtocolButton(
            text = "WireGuard",
            isSelected = selectedProtocol == VpnProtocol.WireGuard,
            onClick = { onProtocolSelected(VpnProtocol.WireGuard) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProtocolButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxHeight(),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent,
            contentColor = if (isSelected) CyanPrimary else TextPrimary.copy(alpha = 0.8f)
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) CyanPrimary else TextPrimary.copy(alpha = 0.3f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Placeholder for protocol icon
            Icon(
                if (text == "OpenVPN") Icons.Default.Info else Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}
