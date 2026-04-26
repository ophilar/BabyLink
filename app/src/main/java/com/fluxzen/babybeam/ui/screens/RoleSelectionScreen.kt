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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // BabyBeam v3 Logo
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.fluxzen.babybeam.R.drawable.v_babybeam_foreground),
            contentDescription = "Logo",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "BabyBeam", style = strategy.typography.headlineLarge, color = strategy.contentColor)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Select Device Role", style = strategy.typography.titleMedium, color = strategy.contentColor.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(32.dp))
        
        strategy.PrimaryButton(
            onClick = { onRoleSelected(true) },
            modifier = Modifier.fillMaxWidth(0.8f),
            content = {
                Text("Baby Station (Sender)")
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        strategy.SecondaryButton(
            onClick = { onRoleSelected(false) },
            modifier = Modifier.fillMaxWidth(0.8f),
            content = {
                Text("Parent Station (Receiver)")
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Unwired component: StellarUI Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = "Powered by",
                style = strategy.typography.labelSmall,
                color = strategy.contentColor.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.fluxzen.babybeam.R.drawable.sui_badge),
                contentDescription = "StellarUI",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

