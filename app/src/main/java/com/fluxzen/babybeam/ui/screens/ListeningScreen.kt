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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fluxzen.babybeam.BabyMonitorViewModel
import com.fluxzen.ui_design.sync.WebRtcManager
import com.fluxzen.ui_design.display.LocalThemeStrategy
import com.fluxzen.ui_design.display.rememberThemeAnimations
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
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
    
    // Mock data removed
    val roomTemp = "--"
    val roomHumidity = "--"

    val isCryDetected by viewModel.isCryDetected.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val visualAlertEnabled by viewModel.visualAlertEnabled.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val remoteVideoTrack by viewModel.webRtcManager.remoteVideoTrackFlow.collectAsState(initial = null)

    val isLightOn by viewModel.isNightLightOn.collectAsState()
    val isMicActive by viewModel.isMicActive.collectAsState()
    val isLullabyPlaying by viewModel.isLullabyPlaying.collectAsState()

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
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BABY MONITOR", style = strategy.typography.titleSmall, letterSpacing = 2.sp, color = strategy.contentColor)
                            Text(connectionStatus, style = MaterialTheme.typography.bodySmall, color = strategy.accentColor)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vib", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = { viewModel.setVibration(it) },
                                modifier = Modifier.semantics { contentDescription = "Toggle Vibration" }.scale(0.7f)
                            )
                            Text("Vis", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = visualAlertEnabled,
                                onCheckedChange = { viewModel.setVisualAlert(it) },
                                modifier = Modifier.semantics { contentDescription = "Toggle Visual Alert" }.scale(0.7f)
                            )
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
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (remoteVideoTrack != null) {
                        VideoRenderer(videoTrack = remoteVideoTrack!!, eglBaseContext = viewModel.webRtcManager.getEglBaseContext())
                    } else {
                        Text("WAITING FOR VIDEO", color = Color.White.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoCard(label = "Temperature", value = "$roomTemp°C", icon = Icons.Default.Thermostat, modifier = Modifier.weight(1f), strategy = strategy)
                    InfoCard(label = "Humidity", value = "$roomHumidity%", icon = Icons.Default.WaterDrop, modifier = Modifier.weight(1f), strategy = strategy)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    ControlButton(
                        icon = Icons.Default.Mic,
                        label = "Talk",
                        isActive = isMicActive,
                        onClick = { viewModel.toggleMic() },
                        strategy = strategy
                    )
                    ControlButton(
                        icon = Icons.Default.MusicNote,
                        label = "Lullaby",
                        isActive = isLullabyPlaying,
                        onClick = { viewModel.toggleLullaby() },
                        strategy = strategy
                    )
                    ControlButton(
                        icon = Icons.Default.Lightbulb,
                        label = "Light",
                        isActive = isLightOn,
                        onClick = { viewModel.toggleNightLight() },
                        strategy = strategy
                    )
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

@Composable
fun VideoRenderer(videoTrack: VideoTrack, eglBaseContext: org.webrtc.EglBase.Context) {
    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglBaseContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
                videoTrack.addSink(this)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}


data class LogEvent(val type: String, val timestamp: Long)

@Composable
fun InfoCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, strategy: com.fluxzen.ui_design.display.ThemeStrategy) {
    strategy.Card(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = label, tint = strategy.accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = strategy.contentColor)
            Text(text = label, style = strategy.typography.bodySmall, color = strategy.contentColor.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isActive: Boolean, onClick: () -> Unit, strategy: com.fluxzen.ui_design.display.ThemeStrategy) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        strategy.SecondaryButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) strategy.backgroundColor else strategy.accentColor,
                modifier = Modifier.background(if (isActive) strategy.accentColor else Color.Transparent, CircleShape)
            )
        }
        Text(text = label, style = strategy.typography.labelSmall, color = strategy.contentColor, modifier = Modifier.padding(top = 4.dp))
    }
}
