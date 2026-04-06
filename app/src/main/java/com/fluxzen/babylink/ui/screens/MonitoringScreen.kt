package com.fluxzen.babylink.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxzen.babylink.BabyMonitorViewModel
import com.fluxzen.ui_design.display.LocalThemeStrategy
import com.fluxzen.ui_design.display.rememberThemeAnimations

@Composable
fun MonitoringScreen(
    viewModel: BabyMonitorViewModel,
    onBack: () -> Unit
) {
    val strategy = LocalThemeStrategy.current
    val animations = rememberThemeAnimations()
    
    // State placeholders (would be driven by service/viewModel)
    val temperature = 21
    val humidity = 45
    val noiseLevel = 0.2f // Normalized 0.0 to 1.0
    val isNightLightOn by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = strategy.backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE MONITORING",
                    color = strategy.accentColor.copy(alpha = 0.7f),
                    style = strategy.typography.labelLarge,
                    letterSpacing = 2.sp
                )
                
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Status",
                        tint = strategy.accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Status Display (Large Temperature)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$temperature°C",
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Light,
                    color = strategy.contentColor
                )
                Text(
                    text = "Nursery Temperature",
                    color = strategy.contentColor.copy(alpha = 0.5f),
                    style = strategy.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(64.dp))

                // Noise Meter (Visualizing Cry Detection Stage 2)
                NoiseMeter(level = noiseLevel, accentColor = strategy.accentColor)
            }

            // Quick Actions & Stats Card
            strategy.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Humidity
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$humidity%", fontWeight = FontWeight.Bold, color = strategy.contentColor)
                        Text(text = "Humidity", style = strategy.typography.bodySmall, color = strategy.contentColor.copy(alpha = 0.5f))
                    }

                    // Night Light Toggle
                    IconButton(
                        onClick = { /* Toggle light */ },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (isNightLightOn) strategy.accentColor else strategy.backgroundColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = "Night Light",
                            tint = if (isNightLightOn) strategy.backgroundColor else strategy.accentColor
                        )
                    }

                    // Connection Quality
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Strong", fontWeight = FontWeight.Bold, color = strategy.accentColor)
                        Text(text = "Signal", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun NoiseMeter(level: Float, accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(level)
                .fillMaxHeight()
                .background(accentColor)
        )
    }
}
