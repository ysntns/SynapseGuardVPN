package com.synapseguard.vpn.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val borderColor = if (isConnected) StatusConnected else BorderPrimary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isConnected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
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
            // Left side - Country flag
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundSecondary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCountryFlag(location),
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Middle - Server info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = serverName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) StatusConnected else TextPrimary
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = StatusConnected,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ping: $pingOrIp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = StatusConnected
                    )
                } else {
                    Text(
                        text = "IP: $pingOrIp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * Gets the country flag emoji based on the location string
 */
private fun getCountryFlag(location: String): String {
    val locationLower = location.lowercase()
    return when {
        locationLower.contains("germany") || locationLower.contains("frankfurt") || locationLower.contains("berlin") -> "🇩🇪"
        locationLower.contains("france") || locationLower.contains("paris") -> "🇫🇷"
        locationLower.contains("uk") || locationLower.contains("london") || locationLower.contains("united kingdom") || locationLower.contains("britain") -> "🇬🇧"
        locationLower.contains("usa") || locationLower.contains("united states") || locationLower.contains("new york") || locationLower.contains("los angeles") || locationLower.contains("chicago") || locationLower.contains("miami") || locationLower.contains("seattle") -> "🇺🇸"
        locationLower.contains("canada") || locationLower.contains("toronto") || locationLower.contains("vancouver") || locationLower.contains("montreal") -> "🇨🇦"
        locationLower.contains("netherlands") || locationLower.contains("amsterdam") -> "🇳🇱"
        locationLower.contains("japan") || locationLower.contains("tokyo") -> "🇯🇵"
        locationLower.contains("singapore") -> "🇸🇬"
        locationLower.contains("australia") || locationLower.contains("sydney") || locationLower.contains("melbourne") -> "🇦🇺"
        locationLower.contains("switzerland") || locationLower.contains("zurich") -> "🇨🇭"
        locationLower.contains("sweden") || locationLower.contains("stockholm") -> "🇸🇪"
        locationLower.contains("turkey") || locationLower.contains("istanbul") || locationLower.contains("ankara") -> "🇹🇷"
        locationLower.contains("brazil") || locationLower.contains("sao paulo") -> "🇧🇷"
        locationLower.contains("india") || locationLower.contains("mumbai") -> "🇮🇳"
        locationLower.contains("south korea") || locationLower.contains("seoul") -> "🇰🇷"
        locationLower.contains("italy") || locationLower.contains("rome") || locationLower.contains("milan") -> "🇮🇹"
        locationLower.contains("spain") || locationLower.contains("madrid") || locationLower.contains("barcelona") -> "🇪🇸"
        locationLower.contains("russia") || locationLower.contains("moscow") -> "🇷🇺"
        locationLower.contains("poland") || locationLower.contains("warsaw") -> "🇵🇱"
        locationLower.contains("austria") || locationLower.contains("vienna") -> "🇦🇹"
        locationLower.contains("ireland") || locationLower.contains("dublin") -> "🇮🇪"
        locationLower.contains("hong kong") -> "🇭🇰"
        locationLower.contains("belgium") || locationLower.contains("brussels") -> "🇧🇪"
        locationLower.contains("norway") || locationLower.contains("oslo") -> "🇳🇴"
        locationLower.contains("finland") || locationLower.contains("helsinki") -> "🇫🇮"
        locationLower.contains("denmark") || locationLower.contains("copenhagen") -> "🇩🇰"
        locationLower.contains("czech") || locationLower.contains("prague") -> "🇨🇿"
        locationLower.contains("portugal") || locationLower.contains("lisbon") -> "🇵🇹"
        locationLower.contains("mexico") || locationLower.contains("mexico city") -> "🇲🇽"
        locationLower.contains("argentina") || locationLower.contains("buenos aires") -> "🇦🇷"
        locationLower.contains("uae") || locationLower.contains("dubai") || locationLower.contains("emirates") -> "🇦🇪"
        locationLower.contains("israel") || locationLower.contains("tel aviv") -> "🇮🇱"
        locationLower.contains("new zealand") || locationLower.contains("auckland") -> "🇳🇿"
        locationLower.contains("taiwan") || locationLower.contains("taipei") -> "🇹🇼"
        locationLower.contains("romania") || locationLower.contains("bucharest") -> "🇷🇴"
        locationLower.contains("ukraine") || locationLower.contains("kyiv") -> "🇺🇦"
        else -> "🌍"
    }
}
