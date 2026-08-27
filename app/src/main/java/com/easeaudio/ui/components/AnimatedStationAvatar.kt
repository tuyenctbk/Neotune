package com.easeaudio.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.easeaudio.R

/**
 * AnimatedStationAvatar renders a station's artwork image with a clean, gentle scale breathing
 * animation whenever [isPlaying] is true.
 *
 * @param imageUrl Stream or station cover artwork URL.
 * @param contentDescription Accessibility description for screen readers.
 * @param isPlaying Whether media is currently playing.
 * @param modifier Composable modifier for sizing and alignment.
 * @param shape Shape outline clipping for the avatar image.
 * @param borderWidth Width of the avatar outer border.
 * @param borderColor Color of the avatar outer border.
 * @param showVinylCenter Deprecated. Kept for backwards compatibility but ignored for a simpler animation.
 * @param enableRotation Deprecated. Kept for backwards compatibility but ignored for a simpler animation.
 */
@Composable
fun AnimatedStationAvatar(
    imageUrl: String?,
    contentDescription: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    borderWidth: Dp = 1.5.dp,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    showVinylCenter: Boolean = false,
    enableRotation: Boolean = false
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl?.ifBlank { null })
            .crossfade(true)
            .error(R.drawable.ic_favicon)
            .placeholder(R.drawable.ic_favicon)
            .build()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SimpleStationAvatarTransition")

    // Simple gentle breathing effect (1.0f -> 1.03f)
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SimpleStationAvatarBreathe"
    )

    val activeScale = if (isPlaying) breatheScale else 1.0f

    Box(
        modifier = modifier
            .scale(activeScale)
            .clip(shape)
            .border(width = borderWidth, color = borderColor, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

