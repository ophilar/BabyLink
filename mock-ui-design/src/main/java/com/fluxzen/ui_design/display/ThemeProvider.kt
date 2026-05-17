package com.fluxzen.ui_design.display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Typography
enum class ThemeVariant { Lullaby }
val LocalThemeStrategy = compositionLocalOf<ThemeStrategy> { error("No theme") }
@Composable fun ThemeProvider(variant: ThemeVariant, mode: Any, content: @Composable () -> Unit) { content() }
@Composable fun rememberThemeAnimations(): Any = Any()

interface ThemeStrategy {
    val backgroundColor: Color
    val contentColor: Color
    val accentColor: Color
    val typography: Typography
    val screenPadding: Dp
    val cornerRadius: Dp
    val cardPadding: Dp

    @Composable fun PrimaryButton(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit)
    @Composable fun SecondaryButton(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit)
    @Composable fun Card(modifier: Modifier, content: @Composable ColumnScope.() -> Unit)
}
