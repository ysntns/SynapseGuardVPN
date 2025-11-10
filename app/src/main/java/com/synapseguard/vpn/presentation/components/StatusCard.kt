package com.synapseguard.vpn.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapseguard.vpn.presentation.theme.*

@Composable
fun StatusCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ServerInfoCard(
    serverName: String,
    location: String,
    pingOrIp: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isConnected) "Connected to:" else "Select Server",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = serverName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) StatusConnected else TextPrimary
                )
                Text(
                    text = location,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isConnected) "Ping" else "IP",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    text = pingOrIp,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) AccentPrimary else TextPrimary
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    downloadSpeed: String,
    uploadSpeed: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundSecondary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Download
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = downloadSpeed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChartDownload
                )
                Text(
                    text = "Download",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // Upload
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uploadSpeed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChartUpload
                )
                Text(
                    text = "Upload",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
