package com.easeaudio.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.easeaudio.ui.theme.NeonCyan
import com.easeaudio.ui.theme.NeonPink
import com.easeaudio.ui.theme.NeonPurple

enum class VisualizerStyle {
    ROUNDED_BARS,
    DUAL_MIRROR,
    WAVE_LINE,
    CIRCULAR_RIPPLE
}

/**
 * A Compose Canvas based Audio Visualizer that reacts to the real-time stream audio frequency
 * amplitudes from Media3 ExoPlayer AudioSessionId.
 */
@Composable
fun AudioVisualizerCanvas(
    waveAmplitudes: List<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    style: VisualizerStyle = VisualizerStyle.ROUNDED_BARS,
    primaryColor: Color = NeonCyan,
    secondaryColor: Color = NeonPurple,
    accentColor: Color = NeonPink
) {
    // Animate each amplitude value smoothly for reactive movement
    val animatedAmplitudes = waveAmplitudes.map { targetAmp ->
        val effectiveTarget = if (isPlaying) targetAmp.coerceIn(0.08f, 1.0f) else 0.05f
        val animatedValue by animateFloatAsState(
            targetValue = effectiveTarget,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "amp_animation"
        )
        animatedValue
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("audio_visualizer_canvas")
    ) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f) return@Canvas

        val barCount = animatedAmplitudes.size.coerceAtLeast(1)
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(accentColor, primaryColor, secondaryColor),
            startY = 0f,
            endY = height
        )

        when (style) {
            VisualizerStyle.ROUNDED_BARS -> {
                val totalSpacing = width * 0.22f
                val barSpacing = totalSpacing / (barCount + 1)
                val barWidth = (width - totalSpacing) / barCount

                animatedAmplitudes.forEachIndexed { index, amp ->
                    val barHeight = (height * 0.85f * amp).coerceAtLeast(4.dp.toPx())
                    val x = barSpacing + index * (barWidth + barSpacing)
                    val y = height - barHeight

                    // Draw main bar
                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Draw floating peak indicator dot on top of bar
                    val peakY = (y - 6.dp.toPx()).coerceAtLeast(2.dp.toPx())
                    drawCircle(
                        color = accentColor,
                        radius = (barWidth * 0.35f).coerceIn(2.dp.toPx(), 4.dp.toPx()),
                        center = Offset(x + barWidth / 2f, peakY)
                    )
                }
            }

            VisualizerStyle.DUAL_MIRROR -> {
                val totalSpacing = width * 0.2f
                val barSpacing = totalSpacing / (barCount + 1)
                val barWidth = (width - totalSpacing) / barCount
                val centerY = height / 2f

                animatedAmplitudes.forEachIndexed { index, amp ->
                    val halfBarHeight = (height * 0.42f * amp).coerceAtLeast(2.dp.toPx())
                    val x = barSpacing + index * (barWidth + barSpacing)
                    val topY = centerY - halfBarHeight
                    val fullBarHeight = halfBarHeight * 2f

                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, fullBarHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }

            VisualizerStyle.WAVE_LINE -> {
                val path = Path()
                val fillPath = Path()
                val stepX = width / (barCount - 1).coerceAtLeast(1)

                path.moveTo(0f, height / 2f)
                fillPath.moveTo(0f, height)
                fillPath.lineTo(0f, height / 2f)

                animatedAmplitudes.forEachIndexed { index, amp ->
                    val x = index * stepX
                    val y = height / 2f + (if (index % 2 == 0) -1 else 1) * (amp * height * 0.42f)
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        val prevX = (index - 1) * stepX
                        val prevY = height / 2f + (if ((index - 1) % 2 == 0) -1 else 1) * (animatedAmplitudes[index - 1] * height * 0.42f)
                        val controlX1 = prevX + (x - prevX) / 2f
                        val controlX2 = controlX1
                        path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                        fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    }
                }

                fillPath.lineTo(width, height)
                fillPath.close()

                // Translucent under-fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.35f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Glowing wave line
                drawPath(
                    path = path,
                    brush = gradientBrush,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            VisualizerStyle.CIRCULAR_RIPPLE -> {
                val centerX = width / 2f
                val centerY = height / 2f
                val maxRadius = (width.coerceAtMost(height) / 2f) * 0.85f

                val avgAmp = animatedAmplitudes.average().toFloat()
                val baseRadius = maxRadius * 0.35f + (avgAmp * maxRadius * 0.2f)

                // Central pulsing core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.6f), primaryColor.copy(alpha = 0.2f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = baseRadius * 1.4f
                    ),
                    center = Offset(centerX, centerY),
                    radius = baseRadius * 1.4f
                )

                // Radial frequency spikes
                val angleStep = (2f * Math.PI / barCount).toFloat()
                animatedAmplitudes.forEachIndexed { index, amp ->
                    val angle = index * angleStep - (Math.PI / 2).toFloat()
                    val spikeLength = amp * (maxRadius - baseRadius)
                    val innerX = centerX + baseRadius * kotlin.math.cos(angle)
                    val innerY = centerY + baseRadius * kotlin.math.sin(angle)
                    val outerX = centerX + (baseRadius + spikeLength) * kotlin.math.cos(angle)
                    val outerY = centerY + (baseRadius + spikeLength) * kotlin.math.sin(angle)

                    drawLine(
                        brush = gradientBrush,
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = (width / (barCount * 3f)).coerceIn(3.dp.toPx(), 8.dp.toPx()),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}
