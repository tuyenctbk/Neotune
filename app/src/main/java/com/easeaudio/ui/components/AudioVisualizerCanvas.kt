package com.easeaudio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.easeaudio.ui.theme.NeonCyan
import com.easeaudio.ui.theme.NeonPink
import com.easeaudio.ui.theme.NeonPurple
import kotlin.math.cos
import kotlin.math.sin

enum class VisualizerStyle {
    ROUNDED_BARS,
    DUAL_MIRROR,
    WAVE_LINE,
    CIRCULAR_RIPPLE,
    NEON_RIBBON,
    DOT_MATRIX
}

/**
 * A Compose Canvas based Audio Visualizer that reacts to the real-time stream audio frequency
 * amplitudes from Media3 ExoPlayer AudioSessionId with fluid physics, peak hold, and glowing effects.
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
    val infiniteTransition = rememberInfiniteTransition(label = "viz_breathing")
    val idlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "idle_phase"
    )

    val rawAmplitudes = remember(waveAmplitudes) {
        if (waveAmplitudes.isEmpty()) List(16) { 0.1f } else waveAmplitudes
    }

    // Peak hold physics state
    val peakAmplitudes = remember { mutableStateListOf<Float>() }
    LaunchedEffect(rawAmplitudes.size) {
        peakAmplitudes.clear()
        rawAmplitudes.forEach { peakAmplitudes.add(it) }
    }

    // Animate each amplitude value smoothly with spring physics
    val animatedAmplitudes = rawAmplitudes.mapIndexed { index, targetAmp ->
        val effectiveTarget = if (isPlaying) {
            targetAmp.coerceIn(0.08f, 1.0f)
        } else {
            // Gentle breathing wave when idle/paused
            val sineWave = (sin(idlePhase + index * 0.45f) * 0.5f + 0.5f) * 0.14f + 0.04f
            sineWave.coerceIn(0.04f, 0.22f)
        }

        // Update peak hold decay
        if (index < peakAmplitudes.size) {
            if (effectiveTarget > peakAmplitudes[index]) {
                peakAmplitudes[index] = effectiveTarget
            } else {
                peakAmplitudes[index] = (peakAmplitudes[index] - 0.015f).coerceAtLeast(effectiveTarget)
            }
        }

        val animatedValue by animateFloatAsState(
            targetValue = effectiveTarget,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "amp_animation_$index"
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
                    val barHeight = (height * 0.82f * amp).coerceAtLeast(4.dp.toPx())
                    val x = barSpacing + index * (barWidth + barSpacing)
                    val y = height - barHeight

                    // Subtle ambient glow behind each bar
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                            startY = y,
                            endY = height
                        ),
                        topLeft = Offset(x - 2.dp.toPx(), y - 2.dp.toPx()),
                        size = Size(barWidth + 4.dp.toPx(), barHeight + 2.dp.toPx()),
                        cornerRadius = CornerRadius(barWidth / 2f + 2.dp.toPx(), barWidth / 2f + 2.dp.toPx())
                    )

                    // Draw main rounded equalizer bar
                    drawRoundRect(
                        brush = gradientBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Draw floating peak indicator dot on top of bar with peak hold physics
                    val peakVal = if (index < peakAmplitudes.size) peakAmplitudes[index] else amp
                    val peakBarHeight = (height * 0.82f * peakVal).coerceAtLeast(4.dp.toPx())
                    val peakY = (height - peakBarHeight - 6.dp.toPx()).coerceAtLeast(2.dp.toPx())

                    drawCircle(
                        color = Color.White,
                        radius = (barWidth * 0.35f).coerceIn(2.dp.toPx(), 4.5.dp.toPx()),
                        center = Offset(x + barWidth / 2f, peakY)
                    )
                }
            }

            VisualizerStyle.DUAL_MIRROR -> {
                val totalSpacing = width * 0.2f
                val barSpacing = totalSpacing / (barCount + 1)
                val barWidth = (width - totalSpacing) / barCount
                val centerY = height / 2f

                // Central subtle hairline glow
                drawLine(
                    color = primaryColor.copy(alpha = 0.35f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 1.dp.toPx()
                )

                animatedAmplitudes.forEachIndexed { index, amp ->
                    val halfBarHeight = (height * 0.44f * amp).coerceAtLeast(2.5.dp.toPx())
                    val x = barSpacing + index * (barWidth + barSpacing)
                    val topY = centerY - halfBarHeight
                    val fullBarHeight = halfBarHeight * 2f

                    // Symmetrical mirrored gradient
                    val mirrorBrush = Brush.verticalGradient(
                        colors = listOf(accentColor, primaryColor, accentColor),
                        startY = topY,
                        endY = topY + fullBarHeight
                    )

                    drawRoundRect(
                        brush = mirrorBrush,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, fullBarHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Dual floating peak dots (top and bottom)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = (barWidth * 0.28f).coerceIn(1.5.dp.toPx(), 3.5.dp.toPx()),
                        center = Offset(x + barWidth / 2f, topY - 3.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = (barWidth * 0.28f).coerceIn(1.5.dp.toPx(), 3.5.dp.toPx()),
                        center = Offset(x + barWidth / 2f, topY + fullBarHeight + 3.dp.toPx())
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

                // Translucent liquid under-fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.15f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Glowing wave line
                drawPath(
                    path = path,
                    brush = Brush.horizontalGradient(listOf(primaryColor, Color.White, accentColor)),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            VisualizerStyle.CIRCULAR_RIPPLE -> {
                val centerX = width / 2f
                val centerY = height / 2f
                val maxRadius = (width.coerceAtMost(height) / 2f) * 0.88f

                val avgAmp = animatedAmplitudes.average().toFloat()
                val baseRadius = maxRadius * 0.35f + (avgAmp * maxRadius * 0.22f)

                // Central breathing aura core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.7f), primaryColor.copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(centerX, centerY),
                        radius = baseRadius * 1.5f
                    ),
                    center = Offset(centerX, centerY),
                    radius = baseRadius * 1.5f
                )

                // Radial frequency rays
                val angleStep = (2f * Math.PI / barCount).toFloat()
                animatedAmplitudes.forEachIndexed { index, amp ->
                    val angle = index * angleStep - (Math.PI / 2).toFloat()
                    val spikeLength = amp * (maxRadius - baseRadius)
                    val innerX = centerX + baseRadius * cos(angle)
                    val innerY = centerY + baseRadius * sin(angle)
                    val outerX = centerX + (baseRadius + spikeLength) * cos(angle)
                    val outerY = centerY + (baseRadius + spikeLength) * sin(angle)

                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor, accentColor, Color.White),
                            start = Offset(innerX, innerY),
                            end = Offset(outerX, outerY)
                        ),
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = (width / (barCount * 2.8f)).coerceIn(3.5.dp.toPx(), 9.dp.toPx()),
                        cap = StrokeCap.Round
                    )
                }
            }

            VisualizerStyle.NEON_RIBBON -> {
                // Multi-layered fluid neon ribbon wave
                val ribbonCount = 3
                for (r in 0 until ribbonCount) {
                    val path = Path()
                    val phaseOffset = r * 1.2f + idlePhase
                    val alpha = if (r == 0) 0.95f else if (r == 1) 0.6f else 0.35f
                    val strokeWidth = if (r == 0) 3.5.dp.toPx() else 2.dp.toPx()
                    val ribbonColor = if (r == 0) primaryColor else if (r == 1) accentColor else secondaryColor

                    val stepX = width / (barCount - 1).coerceAtLeast(1)
                    path.moveTo(0f, height / 2f)

                    animatedAmplitudes.forEachIndexed { index, amp ->
                        val x = index * stepX
                        val harmonic = sin(phaseOffset + index * 0.6f) * (amp * height * 0.38f)
                        val y = height / 2f + harmonic

                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            val prevX = (index - 1) * stepX
                            val prevHarmonic = sin(phaseOffset + (index - 1) * 0.6f) * (animatedAmplitudes[index - 1] * height * 0.38f)
                            val prevY = height / 2f + prevHarmonic
                            val controlX = (prevX + x) / 2f
                            path.cubicTo(controlX, prevY, controlX, y, x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(ribbonColor.copy(alpha = alpha), Color.White.copy(alpha = alpha), ribbonColor.copy(alpha = alpha))),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            VisualizerStyle.DOT_MATRIX -> {
                // Studio-grade LED Dot Matrix
                val dotsPerBar = 7
                val totalSpacing = width * 0.22f
                val barSpacing = totalSpacing / (barCount + 1)
                val barWidth = (width - totalSpacing) / barCount
                val dotSpacing = height * 0.035f
                val dotHeight = (height - (dotsPerBar - 1) * dotSpacing) / dotsPerBar

                animatedAmplitudes.forEachIndexed { colIndex, amp ->
                    val activeDots = (amp * dotsPerBar).toInt().coerceIn(1, dotsPerBar)
                    val x = barSpacing + colIndex * (barWidth + barSpacing)

                    for (rowIndex in 0 until dotsPerBar) {
                        // rowIndex 0 is top, rowIndex dotsPerBar-1 is bottom
                        val fromBottomIndex = (dotsPerBar - 1) - rowIndex
                        val y = rowIndex * (dotHeight + dotSpacing)
                        val isActive = fromBottomIndex < activeDots

                        val dotColor: Color = when {
                            !isActive -> Color.White.copy(alpha = 0.08f)
                            rowIndex == 0 -> Color(0xFFFF3366) // Clipping Red top
                            rowIndex == 1 -> Color(0xFFFFCC00) // Warning Yellow
                            else -> primaryColor // Active Primary
                        }

                        drawRoundRect(
                            color = dotColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, dotHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}
