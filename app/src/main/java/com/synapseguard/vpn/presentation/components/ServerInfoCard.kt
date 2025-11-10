package com.synapseguard.vpn.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapseguard.vpn.presentation.theme.*

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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side - Server icon
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = if (isConnected) StatusConnected else CyanPrimary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Middle - Server info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = serverName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            // Right side - Status indicator
            Column(
                horizontalAlignment = Alignment.End
            ) {
                if (isConnected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Connected",
                        tint = StatusConnected,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = pingOrIp,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isConnected) StatusConnected else TextSecondary
                )
            }
        }
    }
}
