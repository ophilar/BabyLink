package com.fluxzen.babybeam

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.fluxzen.babybeam.navigation.*
import com.fluxzen.babybeam.ui.screens.ListeningScreen
import com.fluxzen.babybeam.ui.screens.MonitoringScreen
import com.fluxzen.babybeam.ui.screens.RoleSelectionScreen
import com.fluxzen.ui_design.audio.ProvideSoundManager
import com.fluxzen.ui_design.display.ThemeProvider
import com.fluxzen.ui_design.display.ThemeVariant
import com.fluxzen.ui_design.navigation.AdaptiveNavScaffold
import com.fluxzen.ui_design.settings.DisplayMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            BabyBeamApp(windowSizeClass = windowSizeClass, activityContext = this@MainActivity)
        }
    }
}

@Composable
fun BabyBeamApp(windowSizeClass: WindowSizeClass, activityContext: Context) {
    val snackbarHostState = remember { SnackbarHostState() }

    ThemeProvider(
        variant = ThemeVariant.Lullaby,
        mode = DisplayMode.DARK
    ) {
        ProvideSoundManager {
            val viewModel: BabyMonitorViewModel = viewModel()
            val backStack = remember { mutableStateListOf<BabyBeamNavKey>(RoleSelectionKey) }

            AdaptiveNavScaffold(
                items = emptyList(), // No bottom nav/rail for this simple flow yet
                activeItemId = null,
                windowSizeClass = windowSizeClass,
                title = "BabyBeam",
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                Surface(modifier = androidx.compose.ui.Modifier.padding(paddingValues)) {
                    val entries = backStack.map { key ->
                        when (key) {
                            RoleSelectionKey -> NavEntry(key) {
                                RoleSelectionScreen(
                                    viewModel = viewModel,
                                    onRoleSelected = { isBaby ->
                                        if (isBaby) {
                                            viewModel.startMonitoring(activityContext)
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

                    NavDisplay(
                        entries = entries,
                        onBack = { backStack.removeLastOrNull() },
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                        }
                    )
                }
            }
        }
    }
}
