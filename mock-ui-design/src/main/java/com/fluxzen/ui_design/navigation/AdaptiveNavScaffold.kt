package com.fluxzen.ui_design.navigation
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.PaddingValues
@Composable fun AdaptiveNavScaffold(
    items: List<Any>, activeItemId: Any?, windowSizeClass: Any, title: String, snackbarHost: @Composable () -> Unit, content: @Composable (PaddingValues) -> Unit
) { content(PaddingValues()) }
