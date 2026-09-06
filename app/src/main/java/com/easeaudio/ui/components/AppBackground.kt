package com.easeaudio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.easeaudio.ui.theme.AppThemeState
import com.easeaudio.ui.theme.ThemePreset

/**
 * Universal ambient cyber-acoustic background modifier.
 * Dynamically renders an elegant multi-stage luminous aura that
 * shifts seamlessly with the active [ThemePreset].
 */
fun Modifier.themeAmbientBackground(theme: ThemePreset? = null): Modifier = this.drawBehind {
    val activeTheme = theme ?: AppThemeState.currentTheme

    // 1. Solid Canvas Foundation
    drawRect(color = activeTheme.background)

    val width = size.width
    val height = size.height

    // 2. Primary Apex Acoustic Bloom (Top-Right / Header Aura)
    val apexCenter = Offset(width * 0.85f, -height * 0.04f)
    val apexRadius = (width * 0.95f).coerceAtLeast(height * 0.45f)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                activeTheme.primary.copy(alpha = 0.12f),
                activeTheme.primary.copy(alpha = 0.04f),
                Color.Transparent
            ),
            center = apexCenter,
            radius = apexRadius
        )
    )

    // 3. Sub-harmonic Secondary Glow (Mid-Left Horizon)
    val midCenter = Offset(-width * 0.15f, height * 0.42f)
    val midRadius = (width * 0.80f).coerceAtLeast(height * 0.38f)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                activeTheme.secondary.copy(alpha = 0.08f),
                activeTheme.secondary.copy(alpha = 0.02f),
                Color.Transparent
            ),
            center = midCenter,
            radius = midRadius
        )
    )

    // 4. Acoustic Floor Warmth (Lower Canvas & Sub-bass Foundation)
    val floorCenter = Offset(width * 0.5f, height * 1.05f)
    val floorRadius = (width * 0.88f).coerceAtLeast(height * 0.42f)
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                activeTheme.tertiary.copy(alpha = 0.07f),
                activeTheme.accent.copy(alpha = 0.02f),
                Color.Transparent
            ),
            center = floorCenter,
            radius = floorRadius
        )
    )

    // 5. Subtle Acoustic Wavefront Horizon Arc (Cyber Radar Rings)
    val arcCenter = Offset(width * 0.5f, -height * 0.20f)
    val arcRadius1 = height * 0.55f
    val arcRadius2 = height * 0.85f

    drawCircle(
        color = activeTheme.primary.copy(alpha = 0.025f),
        radius = arcRadius1,
        center = arcCenter,
        style = Stroke(width = 1.5f)
    )
    drawCircle(
        color = activeTheme.secondary.copy(alpha = 0.020f),
        radius = arcRadius2,
        center = arcCenter,
        style = Stroke(width = 1.2f)
    )
}

/**
 * Fullscreen ambient backdrop composable.
 */
@Composable
fun AppAmbientBackground(
    modifier: Modifier = Modifier,
    theme: ThemePreset? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .themeAmbientBackground(theme = theme),
        content = content
    )
}
