package com.fluxzen.babybeam.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fluxzen.babybeam.BabyMonitorViewModel
import com.fluxzen.ui_design.display.LocalThemeStrategy
import com.fluxzen.ui_design.display.rememberThemeAnimations

@Composable
fun RoleSelectionScreen(
    viewModel: BabyMonitorViewModel,
    onRoleSelected: (Boolean) -> Unit
) {
    val strategy = LocalThemeStrategy.current
    val animations = rememberThemeAnimations()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "BabyBeam", style = strategy.typography.headlineLarge, color = strategy.contentColor)
        Spacer(modifier = Modifier.height(32.dp))
        Text(text = "Select Device Role", style = strategy.typography.titleMedium, color = strategy.contentColor.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(16.dp))
        
        strategy.PrimaryButton(
            onClick = { onRoleSelected(true) },
            modifier = Modifier.fillMaxWidth(0.7f),
            content = {
                Text("Baby Station (Sender)")
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        strategy.SecondaryButton(
            onClick = { onRoleSelected(false) },
            modifier = Modifier.fillMaxWidth(0.7f),
            content = {
                Text("Parent Station (Receiver)")
            }
        )
    }
}

