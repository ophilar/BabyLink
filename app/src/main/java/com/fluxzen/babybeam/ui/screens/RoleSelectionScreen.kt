package com.fluxzen.babybeam.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fluxzen.babybeam.BabyMonitorViewModel
import com.fluxzen.ui_design.display.LocalThemeStrategy
import com.fluxzen.ui_design.display.rememberThemeAnimations

private fun getRequiredPermissions(isSender: Boolean): Array<String> {
    val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
    if (isSender) {
        permissions.add(Manifest.permission.RECORD_AUDIO)
        permissions.add(Manifest.permission.CAMERA)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        permissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(if (isSender) Manifest.permission.BLUETOOTH_ADVERTISE else Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    return permissions.toTypedArray()
}

@Composable
fun RoleSelectionScreen(
    viewModel: BabyMonitorViewModel,
    onRoleSelected: (Boolean) -> Unit
) {
    val strategy = LocalThemeStrategy.current
    val animations = rememberThemeAnimations()

    val senderPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onRoleSelected(true)
        }
    }

    val receiverPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onRoleSelected(false)
        }
    }

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
            onClick = {
                senderPermissionLauncher.launch(getRequiredPermissions(isSender = true))
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            content = {
                Text("Baby Station (Sender)")
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        strategy.SecondaryButton(
            onClick = {
                receiverPermissionLauncher.launch(getRequiredPermissions(isSender = false))
            },
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
