package com.netsentinel.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkCyberScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CyberBg,
    secondary = NeonGreen,
    onSecondary = CyberBg,
    tertiary = NeonPurple,
    onTertiary = CyberBg,
    background = CyberBg,
    onBackground = CyberOnBg,
    surface = CyberSurface,
    onSurface = CyberOnBg,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = CyberOnSurfaceMuted,
    outline = CyberOutline,
    error = ErrorRed,
    onError = CyberBg,
)

/**
 * NetSentinel is dark-only by design (cyberpunk aesthetic is a product requirement),
 * so [isSystemInDarkTheme] is not consulted for color scheme selection.
 */
@Composable
fun NetSentinelTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            it.statusBarColor = CyberBg.toArgb()
            it.navigationBarColor = CyberBg.toArgb()
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkCyberScheme,
        typography = NetSentinelTypography,
        content = content
    )
}
