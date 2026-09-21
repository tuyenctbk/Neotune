package com.easeaudio.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.easeaudio.util.StationLogoResolver
import kotlin.math.abs

/**
 * Curated cyber-neon and audiophile gradient palettes for station artwork fallbacks.
 */
data class StationGradientPreset(
    val name: String,
    val colors: List<Color>,
    val accentColor: Color
)

object StationGradientPalette {
    val presets = listOf(
        StationGradientPreset(
            name = "Cyber Cyan",
            colors = listOf(Color(0xFF062035), Color(0xFF0284C7), Color(0xFF00F0FF)),
            accentColor = Color(0xFF00F0FF)
        ),
        StationGradientPreset(
            name = "Synthwave Fuchsia",
            colors = listOf(Color(0xFF3B0764), Color(0xFF9333EA), Color(0xFFEC4899)),
            accentColor = Color(0xFFEC4899)
        ),
        StationGradientPreset(
            name = "Sunset Amber",
            colors = listOf(Color(0xFF451A03), Color(0xFFEA580C), Color(0xFFFBBF24)),
            accentColor = Color(0xFFFBBF24)
        ),
        StationGradientPreset(
            name = "Electric Emerald",
            colors = listOf(Color(0xFF022C22), Color(0xFF059669), Color(0xFF34D399)),
            accentColor = Color(0xFF34D399)
        ),
        StationGradientPreset(
            name = "Ultraviolet",
            colors = listOf(Color(0xFF1E1B4B), Color(0xFF4338CA), Color(0xFF818CF8)),
            accentColor = Color(0xFF818CF8)
        ),
        StationGradientPreset(
            name = "Neon Rose",
            colors = listOf(Color(0xFF4C0519), Color(0xFFE11D48), Color(0xFFFB7185)),
            accentColor = Color(0xFFFB7185)
        ),
        StationGradientPreset(
            name = "Cosmic Purple",
            colors = listOf(Color(0xFF18042B), Color(0xFF7C3AED), Color(0xFFC084FC)),
            accentColor = Color(0xFFC084FC)
        ),
        StationGradientPreset(
            name = "Oceanic Teal",
            colors = listOf(Color(0xFF042F2E), Color(0xFF0D9488), Color(0xFF2DD4BF)),
            accentColor = Color(0xFF2DD4BF)
        ),
        StationGradientPreset(
            name = "Solar Flame",
            colors = listOf(Color(0xFF381500), Color(0xFFD97706), Color(0xFFFDE047)),
            accentColor = Color(0xFFFDE047)
        ),
        StationGradientPreset(
            name = "Sonic Blue",
            colors = listOf(Color(0xFF08204D), Color(0xFF2563EB), Color(0xFF38BDF8)),
            accentColor = Color(0xFF38BDF8)
        ),
        StationGradientPreset(
            name = "Cyber Coral",
            colors = listOf(Color(0xFF3B081A), Color(0xFFE11D48), Color(0xFFFB923C)),
            accentColor = Color(0xFFFB923C)
        ),
        StationGradientPreset(
            name = "Aurora Mint",
            colors = listOf(Color(0xFF022819), Color(0xFF10B981), Color(0xFF6EE7B7)),
            accentColor = Color(0xFF6EE7B7)
        )
    )

    fun getPresetForStation(name: String, genre: String = ""): StationGradientPreset {
        val clean = (name.trim().lowercase() + genre.trim().lowercase())
        val seed = if (clean.isBlank()) "neotune".hashCode() else clean.hashCode()
        val index = abs(seed) % presets.size
        return presets[index]
    }
}

/**
 * Generates a colorful, high-definition gradient placeholder for any station or podcast.
 * Includes deterministic color palette, vinyl groove concentric arcs, station initials,
 * audio watermark glyph, and optional animated shimmer (during loading) or equalizer bars (while playing).
 */
@Composable
fun StationGradientPlaceholder(
    stationName: String,
    modifier: Modifier = Modifier,
    genre: String = "",
    isPodcast: Boolean = false,
    shape: Shape? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    isPlaying: Boolean = false,
    isLoading: Boolean = false
) {
    val safeName = stationName.ifBlank { if (isPodcast) "Podcast" else "Radio" }
    val preset = remember(safeName, genre) {
        StationGradientPalette.getPresetForStation(safeName, genre)
    }

    val initials = remember(safeName) {
        val clean = safeName.trim().replace(Regex("""\([^)]*\)"""), "").trim()
        val parts = clean.split(Regex("""[\s\-_.]+""")).filter { it.isNotBlank() }
        when {
            parts.isEmpty() -> "♪"
            parts[0].length in 2..4 && parts[0].all { it.isUpperCase() || it.isDigit() } -> parts[0]
            parts.size >= 2 -> "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
            clean.length >= 2 -> clean.take(2).uppercase()
            else -> clean.uppercase()
        }
    }

    val isActuallyPodcast = isPodcast || genre.contains("podcast", ignoreCase = true) || safeName.contains("podcast", ignoreCase = true)

    // Shimmer sweep animation when in loading state
    val infiniteTransition = rememberInfiniteTransition(label = "GradientPlaceholderAnim")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerSweep"
    )

    // Mini equalizer bounce when playing
    val eq1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val eq2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val eq3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(370, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    BoxWithConstraints(
        modifier = modifier
            .then(if (shape != null) Modifier.clip(shape) else Modifier)
            .background(Brush.linearGradient(colors = preset.colors)),
        contentAlignment = Alignment.Center
    ) {
        val minDim = minOf(maxWidth, maxHeight)

        // Vinyl soundwave grooves & radial light depth
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width * 0.5f, size.height * 0.5f)
            val maxR = maxOf(size.width, size.height) * 0.7f

            // Concentric soundwave arcs
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = maxR * 0.4f,
                center = centerOffset,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = maxR * 0.65f,
                center = centerOffset,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = maxR * 0.9f,
                center = centerOffset,
                style = Stroke(width = 1.0.dp.toPx())
            )

            // Top-left ambient specular reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.25f, size.height * 0.25f),
                    radius = size.width * 0.55f
                )
            )

            // Animated shimmer sweep if loading
            if (isLoading) {
                val shimmerStart = Offset(size.width * shimmerOffset, 0f)
                val shimmerEnd = Offset(size.width * (shimmerOffset + 0.5f), size.height)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        start = shimmerStart,
                        end = shimmerEnd
                    )
                )
            }
        }

        // Watermark Icon in background
        val watermarkRatio = if (minDim >= 60.dp) 0.82f else 0.72f
        val watermarkOffset = if (minDim >= 60.dp) 5.dp else 2.dp

        Icon(
            imageVector = if (isActuallyPodcast) Icons.Filled.Mic else Icons.Filled.Radio,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxSize(watermarkRatio)
                .align(Alignment.BottomEnd)
                .offset(x = watermarkOffset, y = watermarkOffset)
        )

        // Station Initials Monogram
        val calculatedFontSize = when {
            fontSize != TextUnit.Unspecified -> fontSize
            minDim >= 140.dp -> 36.sp
            minDim >= 100.dp -> 28.sp
            minDim >= 70.dp -> 20.sp
            minDim >= 50.dp -> 15.sp
            minDim >= 36.dp -> 12.sp
            else -> 10.sp
        }

        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = calculatedFontSize,
                color = Color.White,
                letterSpacing = if (initials.length > 2) 0.sp else 0.5.sp
            ),
            maxLines = 1
        )

        // Playing Live Equalizer Mini-Bars in bottom-start corner
        if (isPlaying && minDim >= 50.dp) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(10.dp * eq1)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(1.dp))
                )
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(10.dp * eq2)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(1.dp))
                )
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(10.dp * eq3)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

/**
 * Universal station artwork loader with intelligent fallback to a colorful gradient placeholder.
 *
 * 1. Checks and resolves URL via [StationLogoResolver].
 * 2. Fetches via Coil with crossfade enabled.
 * 3. While loading, displays [StationGradientPlaceholder] with a smooth luminous shimmer sweep.
 * 4. On Coil failure (HTTP error, connection timeout, missing art), automatically displays
 *    the station's unique colorful gradient placeholder.
 * 5. Smoothly crossfades between placeholder and loaded artwork.
 */
@Composable
fun StationArtLoader(
    imageUrl: String?,
    stationName: String,
    modifier: Modifier = Modifier,
    genre: String = "",
    isPodcast: Boolean = false,
    contentDescription: String? = stationName,
    shape: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.Crop,
    isPlaying: Boolean = false,
    borderWidth: Dp = 0.dp,
    borderColor: Color = Color.Transparent
) {
    val context = LocalContext.current
    val resolvedUrl = remember(imageUrl, stationName, genre) {
        if (!imageUrl.isNullOrBlank()) {
            imageUrl
        } else {
            StationLogoResolver.resolveStationLogo(
                name = stationName,
                favicon = "",
                homepage = "",
                tags = genre
            )
        }
    }

    val imageRequest = remember(resolvedUrl) {
        ImageRequest.Builder(context)
            .data(resolvedUrl.ifBlank { null })
            .crossfade(300)
            .build()
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (borderWidth > 0.dp) Modifier.border(borderWidth, borderColor, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        ) {
            when (val state = painter.state) {
                is AsyncImagePainter.State.Success -> {
                    SubcomposeAsyncImageContent()
                }
                is AsyncImagePainter.State.Loading -> {
                    StationGradientPlaceholder(
                        stationName = stationName,
                        genre = genre,
                        isPodcast = isPodcast,
                        shape = shape,
                        isPlaying = isPlaying,
                        isLoading = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is AsyncImagePainter.State.Error -> {
                    // Guaranteed fallback mechanism: renders custom colorful gradient placeholder
                    StationGradientPlaceholder(
                        stationName = stationName,
                        genre = genre,
                        isPodcast = isPodcast,
                        shape = shape,
                        isPlaying = isPlaying,
                        isLoading = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    StationGradientPlaceholder(
                        stationName = stationName,
                        genre = genre,
                        isPodcast = isPodcast,
                        shape = shape,
                        isPlaying = isPlaying,
                        isLoading = false,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
