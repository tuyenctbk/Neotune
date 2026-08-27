package com.easeaudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.easeaudio.service.SystemThemeService

@Composable
fun TuneveTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemThemeService = rememberSystemThemeService(context)
    val isSystemDarkFromService by systemThemeService.isSystemDarkTheme.collectAsState()
    val isSystemDarkCompose = isSystemInDarkTheme()

    val currentSystemDark = isSystemDarkFromService || isSystemDarkCompose

    LaunchedEffect(currentSystemDark) {
        AppThemeState.updateSystemDarkTheme(currentSystemDark)
    }

    val isLight = AppThemeState.isLightMode
    val colorScheme = if (isLight) {
        lightColorScheme(
            primary = NeonCyan,
            onPrimary = Color.White,
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
    } else {
        darkColorScheme(
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
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DarkBackground.toArgb()
                window.navigationBarColor = DarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = isLight
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
private fun rememberSystemThemeService(context: android.content.Context): SystemThemeService {
    return androidx.compose.runtime.remember(context) {
        SystemThemeService.getInstance(context)
    }
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
