package com.fluxzen.babybeam.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxzen.babybeam.BabyMonitorViewModel
import com.fluxzen.ui_design.display.LocalThemeStrategy
import com.fluxzen.ui_design.display.rememberThemeAnimations
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeningScreen(
    viewModel: BabyMonitorViewModel,
    onBack: () -> Unit
) {
    val strategy = LocalThemeStrategy.current
    val animations = rememberThemeAnimations()
    
    // Mock data
    val roomTemp = 21
    val roomHumidity = 45
    val historyEvents = remember {
        mutableStateListOf(
            LogEvent("Cry Detected", System.currentTimeMillis() - 3600000),
            LogEvent("Motion Alert", System.currentTimeMillis() - 7200000)
        )
    }
    val isCryDetected by viewModel.isCryDetected.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val visualAlertEnabled by viewModel.visualAlertEnabled.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize().background(strategy.backgroundColor)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("BABY MONITOR", style = strategy.typography.titleSmall, letterSpacing = 2.sp, color = strategy.contentColor) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vib", style = MaterialTheme.typography.labelSmall)
                            Switch(checked = vibrationEnabled, onCheckedChange = { viewModel.setVibration(it) }, modifier = Modifier.scale(0.7f))
                            Text("Vis", style = MaterialTheme.typography.labelSmall)
                            Switch(checked = visualAlertEnabled, onCheckedChange = { viewModel.setVisualAlert(it) }, modifier = Modifier.scale(0.7f))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.Black), // Live stream window usually stays black
                    contentAlignment = Alignment.Center
                ) {
                    Text("LIVE AUDIO STREAM", color = Color.White.copy(alpha = 0.5f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard(label = "Temperature", value = "$roomTemp°C", icon = Icons.Default.Thermostat, modifier = Modifier.weight(1f), strategy = strategy)
                    InfoCard(label = "Humidity", value = "$roomHumidity%", icon = Icons.Default.WaterDrop, modifier = Modifier.weight(1f), strategy = strategy)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    ControlButton(icon = Icons.Default.Mic, label = "Talk", strategy = strategy)
                    ControlButton(icon = Icons.Default.MusicNote, label = "Lullaby", strategy = strategy)
                    ControlButton(icon = Icons.Default.Lightbulb, label = "Light", strategy = strategy)
                }
            }
        }

        if (isCryDetected && visualAlertEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = alpha))
                    .clickable { viewModel.dismissAlert() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Cry Detected",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "BABY CRYING",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Tap to dismiss",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}


data class LogEvent(val type: String, val timestamp: Long)

@Composable
fun InfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, strategy: com.fluxzen.ui_design.display.ThemeStrategy) {
    strategy.Card(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = strategy.accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = strategy.contentColor)
            Text(text = label, style = strategy.typography.bodySmall, color = strategy.contentColor.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, strategy: com.fluxzen.ui_design.display.ThemeStrategy) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        strategy.SecondaryButton(
            onClick = { /* Action */ },
            modifier = Modifier
                .size(56.dp)
        ) {
            Icon(icon, contentDescription = label, tint = strategy.accentColor)
        }
        Text(text = label, style = strategy.typography.labelSmall, color = strategy.contentColor, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun HistoryItem(event: LogEvent) {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = event.type, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(text = sdf.format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

