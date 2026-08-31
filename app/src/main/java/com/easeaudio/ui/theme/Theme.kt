package com.easeaudio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun TuneveTheme(
    content: @Composable () -> Unit
) {
    // App is always dark. No system-theme tracking or light-mode logic.
    val colorScheme = darkColorScheme(
        primary = NeonCyan,
        onPrimary = DarkBackground,
        primaryContainer = ActivePill,
        onPrimaryContainer = NeonCyan,
        secondary = NeonPurple,
        onSecondary = TextPrimary,
        tertiary = NeonPink,
        background = DarkBackground,
        onBackground = TextPrimary,
        surface = DarkSurface,
        onSurface = TextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = CardBorder
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                // Always dark — light icon tints on bars
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun EaseAudioTheme(content: @Composable () -> Unit) = TuneveTheme(content = content)

@Composable
fun rememberIsTv(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember(context) {
        val uiModeManager = context.getSystemService(android.content.Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        val isTvMode = uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        val hasTvHardware = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEVISION)
        isTvMode || hasLeanback || hasTvHardware
    }
}

