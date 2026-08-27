package com.easeaudio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * Custom Composable Modifier that applies a subtle, infinite scale-pulse animation
 * to Station Avatar image components within player UI elements whenever [isPlaying] is true.
 *
 * @param isPlaying Controls whether the scale-pulse animation is active.
 * @param pulseTargetScale The peak scaling multiplier applied at maximum pulse expansion.
 * @param durationMillis The duration of one expansion or contraction pulse cycle.
 */
@Composable
fun Modifier.pulseOnPlaying(
    isPlaying: Boolean,
    pulseTargetScale: Float = 1.06f,
    durationMillis: Int = 1200
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseOnPlayingTransition")
    val animatedPulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = pulseTargetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseOnPlayingScale"
    )

    val scaleFactor = if (isPlaying) animatedPulseScale else 1.0f
    return this.then(Modifier.scale(scaleFactor))
}
