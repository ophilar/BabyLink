package com.fluxzen.babylink

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.fluxzen.babylink.navigation.*
import com.fluxzen.babylink.ui.screens.ListeningScreen
import com.fluxzen.babylink.ui.screens.MonitoringScreen
import com.fluxzen.babylink.ui.screens.RoleSelectionScreen
import com.fluxzen.ui_design.audio.ProvideSoundManager
import com.fluxzen.ui_design.display.ThemeProvider
import com.fluxzen.ui_design.display.ThemeVariant
import com.fluxzen.ui_design.navigation.AdaptiveNavScaffold
import com.fluxzen.ui_design.settings.DisplayMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestPermissions()
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val snackbarHostState = remember { SnackbarHostState() }
            
            ThemeProvider(
                variant = ThemeVariant.Lullaby,
                mode = DisplayMode.DARK
            ) {
                ProvideSoundManager {
                    val viewModel: BabyMonitorViewModel = viewModel()
                    val backStack = remember { mutableStateListOf<BabyLinkNavKey>(RoleSelectionKey) }

                    AdaptiveNavScaffold(
                        items = emptyList(), // No bottom nav/rail for this simple flow yet
                        activeItemId = null,
                        windowSizeClass = windowSizeClass,
                        title = "BabyLink",
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { paddingValues ->
                        Surface(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
                            NavDisplay(
                                backStack = backStack,
                                onBack = { backStack.removeLastOrNull() },
                                transitionSearch = { _, _ ->
                                    // Custom premium transitions
                                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                                }
                            ) { key ->
                                when (key) {
                                    RoleSelectionKey -> NavEntry(key) {
                                        RoleSelectionScreen(
                                            viewModel = viewModel,
                                            onRoleSelected = { isBaby ->
                                                if (isBaby) {
                                                    viewModel.startMonitoring(this@MainActivity)
                                                    backStack.navigateSingleTop(MonitoringKey)
                                                } else {
                                                    viewModel.startDiscovery()
                                                    backStack.navigateSingleTop(ListeningKey)
                                                }
                                            }
                                        )
                                    }
                                    MonitoringKey -> NavEntry(key) {
                                        MonitoringScreen(
                                            viewModel = viewModel,
                                            onBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                    ListeningKey -> NavEntry(key) {
                                        ListeningScreen(
                                            viewModel = viewModel,
                                            onBack = { backStack.removeLastOrNull() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }
}
