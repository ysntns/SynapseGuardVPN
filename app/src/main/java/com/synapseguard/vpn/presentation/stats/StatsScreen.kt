package com.synapseguard.vpn.presentation.stats

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapseguard.vpn.presentation.theme.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BackgroundPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Connection Stats",
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Speed Gauge
            item {
                SpeedGaugeCard(
                    downloadSpeed = uiState.downloadSpeed,
                    totalDataUsed = uiState.totalDataUsed
                )
            }

            // Download/Upload Speed Bars
            item {
                SpeedBarsCard(
                    downloadSpeed = uiState.downloadSpeed,
                    uploadSpeed = uiState.uploadSpeed
                )
            }

            // Real-time Speed Graph
            item {
                RealTimeSpeedGraphCard(
                    speedHistory = uiState.speedHistory
                )
            }

            // Data Usage Graph
            item {
                DataUsageGraphCard(
                    dataHistory = uiState.dataUsageHistory
                )
            }

            // BCI Neural Latency
            item {
                NeuralLatencyCard(
                    latency = uiState.neuralLatency
                )
            }

            // Speed Test Button
            item {
                SpeedTestCard(
                    isRunning = uiState.isRunningSpeedTest,
                    result = uiState.speedTestResult,
                    onRunTest = { viewModel.runSpeedTest() }
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SpeedGaugeCard(
    downloadSpeed: Long,
    totalDataUsed: Long
) {
    val speedMbps = (downloadSpeed * 8.0 / 1_000_000).toInt() // Convert bytes/s to Mbps

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Speed",
                fontSize = 16.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular speed gauge
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularSpeedGauge(speedMbps = speedMbps)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$speedMbps",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanPrimary
                    )
                    Text(
                        text = "Mbps",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = formatBytes(totalDataUsed),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "Total Data Used",
                fontSize = 14.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun CircularSpeedGauge(speedMbps: Int) {
    val maxSpeed = 200
    val progress = (speedMbps.toFloat() / maxSpeed).coerceIn(0f, 1f)

    // Animate progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "speed_progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 20.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Background arc
        drawArc(
            color = BackgroundSecondary,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = CyanPrimary,
            startAngle = 135f,
            sweepAngle = 270f * animatedProgress,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun SpeedBarsCard(
    downloadSpeed: Long,
    uploadSpeed: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Speed Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Download
            SpeedBar(
                label = "Download",
                speed = downloadSpeed,
                color = ChartDownload
            )

            // Upload
            SpeedBar(
                label = "Upload",
                speed = uploadSpeed,
                color = ChartUpload
            )
        }
    }
}

@Composable
private fun SpeedBar(
    label: String,
    speed: Long,
    color: Color
) {
    val speedMbps = speed * 8.0 / 1_000_000 // Convert bytes/s to Mbps
    val progress = (speedMbps / 200.0).toFloat().coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = TextSecondary
            )
            Text(
                text = String.format("%.1f Mbps", speedMbps),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            // Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .androidx.compose.foundation.background(BackgroundSecondary)
            )

            // Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .androidx.compose.foundation.background(color)
            )
        }
    }
}

@Composable
private fun RealTimeSpeedGraphCard(
    speedHistory: List<SpeedHistoryPoint>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Real-Time Speed (Last 60s)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .androidx.compose.foundation.background(ChartDownload)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Download",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .androidx.compose.foundation.background(ChartUpload)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Upload",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            // Real-time line chart
            if (speedHistory.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    RealTimeSpeedChart(speedHistory = speedHistory)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Connect to VPN to see live speed data",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RealTimeSpeedChart(speedHistory: List<SpeedHistoryPoint>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (speedHistory.size < 2) return@Canvas

        val maxSpeed = speedHistory.maxOfOrNull {
            maxOf(it.downloadSpeedMbps, it.uploadSpeedMbps)
        }?.coerceAtLeast(1.0) ?: 1.0

        val width = size.width
        val height = size.height
        val spacing = width / (speedHistory.size - 1).coerceAtLeast(1)

        // Draw grid lines
        val gridColor = BackgroundSecondary
        for (i in 0..4) {
            val y = height * i / 4
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw download speed path
        val downloadPath = Path().apply {
            speedHistory.forEachIndexed { index, point ->
                val x = index * spacing
                val y = height - (point.downloadSpeedMbps.toFloat() / maxSpeed.toFloat() * height)

                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        drawPath(
            path = downloadPath,
            color = ChartDownload,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw upload speed path
        val uploadPath = Path().apply {
            speedHistory.forEachIndexed { index, point ->
                val x = index * spacing
                val y = height - (point.uploadSpeedMbps.toFloat() / maxSpeed.toFloat() * height)

                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        drawPath(
            path = uploadPath,
            color = ChartUpload,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw points for download
        speedHistory.forEachIndexed { index, point ->
            val x = index * spacing
            val yDownload = height - (point.downloadSpeedMbps.toFloat() / maxSpeed.toFloat() * height)

            drawCircle(
                color = ChartDownload,
                radius = 3.dp.toPx(),
                center = Offset(x, yDownload)
            )
        }

        // Draw points for upload
        speedHistory.forEachIndexed { index, point ->
            val x = index * spacing
            val yUpload = height - (point.uploadSpeedMbps.toFloat() / maxSpeed.toFloat() * height)

            drawCircle(
                color = ChartUpload,
                radius = 3.dp.toPx(),
                center = Offset(x, yUpload)
            )
        }
    }
}

@Composable
private fun DataUsageGraphCard(
    dataHistory: List<DataUsagePoint>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Data Usage (This Month)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Simple line chart
            if (dataHistory.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    SimpleLineChart(dataPoints = dataHistory)
                }
            }
        }
    }
}

@Composable
private fun SimpleLineChart(dataPoints: List<DataUsagePoint>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (dataPoints.size < 2) return@Canvas

        val maxValue = dataPoints.maxOfOrNull { it.bytesUsed } ?: 1L
        val width = size.width
        val height = size.height
        val spacing = width / (dataPoints.size - 1)

        val path = Path().apply {
            dataPoints.forEachIndexed { index, point ->
                val x = index * spacing
                val y = height - (point.bytesUsed.toFloat() / maxValue * height)

                if (index == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
        }

        drawPath(
            path = path,
            color = CyanPrimary,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw points
        dataPoints.forEachIndexed { index, point ->
            val x = index * spacing
            val y = height - (point.bytesUsed.toFloat() / maxValue * height)

            drawCircle(
                color = CyanPrimary,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun NeuralLatencyCard(latency: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "BCI Network Latency",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Neural processing optimized",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${latency}ms",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        latency < 15 -> StatusConnected
                        latency < 25 -> StatusConnecting
                        else -> StatusDisconnected
                    }
                )
                Text(
                    text = when {
                        latency < 15 -> "Excellent"
                        latency < 25 -> "Good"
                        else -> "Fair"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SpeedTestCard(
    isRunning: Boolean,
    result: SpeedTestResult?,
    onRunTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = BackgroundCard
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Speed Test",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (result != null && !isRunning) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f", result.downloadMbps),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChartDownload
                        )
                        Text("Mbps ↓", fontSize = 12.sp, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f", result.uploadMbps),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ChartUpload
                        )
                        Text("Mbps ↑", fontSize = 12.sp, color = TextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.ping}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusConnected
                        )
                        Text("ms ping", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            Button(
                onClick = onRunTest,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = BackgroundPrimary
                )
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "Running Test..." else "Run Speed Test",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
