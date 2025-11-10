package com.synapseguard.vpn.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synapseguard.vpn.domain.model.VpnServer
import com.synapseguard.vpn.presentation.theme.*

@Composable
fun ServerListItem(
    server: VpnServer,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceSelected else BackgroundSecondary
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, BorderAccent)
        } else {
            BorderStroke(1.dp, BorderPrimary)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Flag + Server info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Country flag placeholder (will be replaced with actual flag)
                Text(
                    text = getFlagEmoji(server.countryCode),
                    fontSize = 32.sp
                )

                Column {
                    Text(
                        text = server.city,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = server.country,
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }

            // Right side: Stats + Selection indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Server load
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${server.load}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = getLoadColor(server.load)
                    )
                    Text(
                        text = "${server.latency}ms",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                // Selection indicator
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = CyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get flag emoji for country code
 */
private fun getFlagEmoji(countryCode: String): String {
    // Convert country code to flag emoji
    return countryCode.uppercase()
        .map { char -> Character.codePointAt("$char", 0) - 0x41 + 0x1F1E6 }
        .map { codePoint -> Character.toChars(codePoint) }
        .joinToString("") { String(it) }
}

/**
 * Get color based on server load
 */
private fun getLoadColor(load: Int): androidx.compose.ui.graphics.Color {
    return when {
        load < 30 -> StatusConnected // Green - Low load
        load < 70 -> StatusConnecting // Yellow - Medium load
        else -> StatusDisconnected // Red - High load
    }
}
