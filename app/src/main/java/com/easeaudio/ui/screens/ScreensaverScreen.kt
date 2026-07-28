package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

enum class AmbientTheme(@StringRes val nameRes: Int) {
    STATION_ART(R.string.theme_station_cover),
    AURORA(R.string.theme_aurora),
    COSMIC_NIGHT(R.string.theme_cosmic),
    OLED_MINIMAL(R.string.theme_oled),
    PITCH_BLACK(R.string.theme_pitch_black)
}

@Composable
fun ScreensaverScreen(
    currentStation: RadioStation?,
    isPlaying: Boolean,
    streamTitle: String?,
    waveAmplitudes: List<Float>,
    sleepTimerRemaining: Int?,
    onTogglePlay: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getFormattedTime()) }
    var currentDate by remember { mutableStateOf(getFormattedDate()) }
    var selectedTheme by remember { mutableStateOf(AmbientTheme.STATION_ART) }
    var isDimmed by remember { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(true) }

    LaunchedEffect(showOverlayControls) {
        if (showOverlayControls) {
            delay(6000L)
            showOverlayControls = false
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getFormattedTime()
            currentDate = getFormattedDate()
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "burnInDrift")
    val driftX by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftX"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftY"
    )

    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showOverlayControls = !showOverlayControls
            }
    ) {
        val maxScreenHeight = maxHeight
        val isCompactHeight = maxScreenHeight < 680.dp

        // LAYER 1: Full Screen Background Image / Ambient Theme
        when (selectedTheme) {
            AmbientTheme.STATION_ART -> {
                if (currentStation != null && currentStation.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = currentStation.imageUrl,
                        contentDescription = "Background Cover Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(if (isDimmed) 12.dp else 6.dp)
                    )
                } else {
                    AuroraGradientCanvas(particlePhase)
                }
            }
            AmbientTheme.AURORA -> AuroraGradientCanvas(particlePhase)
            AmbientTheme.COSMIC_NIGHT -> CosmicNightCanvas(particlePhase)
            AmbientTheme.OLED_MINIMAL, AmbientTheme.PITCH_BLACK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }

        val dimAlpha = if (isDimmed) 0.85f else 0.55f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = dimAlpha + 0.1f),
                            Color.Black.copy(alpha = dimAlpha - 0.15f),
                            Color.Black.copy(alpha = dimAlpha + 0.25f)
                        )
                    )
                )
        )

        // LAYER 2: Primary Adaptive Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = if (isCompactHeight) 16.dp else 24.dp,
                    vertical = if (isCompactHeight) 12.dp else 20.dp
                )
                .offset(x = driftX.dp, y = driftY.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Sleep Timer & Dim Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sleepTimerRemaining != null) {
                    Surface(
                        color = NeonPurple.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { onOpenSleepTimer() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = NeonPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.sleeping_in, sleepTimerRemaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                IconButton(
                    onClick = { isDimmed = !isDimmed },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .testTag("btn_ambient_dim")
                ) {
                    Icon(
                        imageVector = if (isDimmed) Icons.Filled.LightMode else Icons.Outlined.BrightnessMedium,
                        contentDescription = stringResource(R.string.night_dim),
                        tint = if (isDimmed) NeonCyan else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

            // Center Column: Clock & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = if (isCompactHeight) 8.dp else 16.dp)
            ) {
                Text(
                    text = currentTime,
                    fontSize = if (isCompactHeight) 48.sp else 68.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = TextPrimary.copy(alpha = if (isDimmed) 0.7f else 0.95f),
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = currentDate,
                    style = if (isCompactHeight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = NeonCyan.copy(alpha = if (isDimmed) 0.6f else 0.85f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

            // Bottom Section: Now Playing Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentStation != null) {
                    Surface(
                        color = DarkSurface.copy(alpha = if (isDimmed) 0.5f else 0.8f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dock_now_playing_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 12.dp else 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = streamTitle ?: currentStation.genre,
                                    style = if (isCompactHeight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentStation.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("•", color = TextMuted)
                                Text(
                                    text = currentStation.bitrate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonPurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Waveform Visualizer
                            Row(
                                modifier = Modifier
                                    .height(22.dp)
                                    .fillMaxWidth(0.5f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                waveAmplitudes.forEach { amp ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight(amp.coerceIn(0.12f, 1.0f))
                                            .clip(CircleShape)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(NeonCyan, NeonPurple, NeonPink)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        color = DarkSurface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tap_tuner_pick_station),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = !showOverlayControls,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = stringResource(R.string.tap_for_ambient_controls),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // LAYER 3: Touch-to-Reveal Minimal Ambient Control Bar (Scrollable chips)
        AnimatedVisibility(
            visible = showOverlayControls,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.88f),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth(0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable Theme Selector Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wallpaper,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 2.dp)
                        )

                        AmbientTheme.entries.forEach { theme ->
                            val isSelected = selectedTheme == theme
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTheme = theme },
                                label = {
                                    Text(
                                        text = stringResource(theme.nameRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ActivePill,
                                    selectedLabelColor = DarkBackground,
                                    containerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = CardBorder,
                                    selectedBorderColor = NeonCyan
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimal Transport Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenSleepTimer,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (sleepTimerRemaining != null) ActivePill else DarkSurfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bedtime,
                                contentDescription = stringResource(R.string.sleep_timer),
                                tint = if (sleepTimerRemaining != null) DarkBackground else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        FloatingActionButton(
                            onClick = onTogglePlay,
                            containerColor = NeonCyan,
                            contentColor = DarkBackground,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dock_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (onToggleFavorite != null && currentStation != null) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = if (currentStation.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (currentStation.isFavorite) NeonPink else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuroraGradientCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F0826),
                    Color(0xFF071328),
                    Color(0xFF020914)
                )
            )
        )

        val xOffset1 = (sin(phase) * 150f)
        val yOffset1 = (sin(phase * 0.7f) * 100f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonPurple.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(width * 0.3f + xOffset1, height * 0.35f + yOffset1),
                radius = width * 0.8f
            )
        )

        val xOffset2 = (sin(phase * 1.3f) * 180f)
        val yOffset2 = (sin(phase * 0.9f) * 120f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonCyan.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(width * 0.7f + xOffset2, height * 0.65f + yOffset2),
                radius = width * 0.75f
            )
        )
    }
}

@Composable
private fun CosmicNightCanvas(phase: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF03050B),
                    Color(0xFF0B0D1A),
                    Color(0xFF020307)
                )
            )
        )

        for (i in 0..40) {
            val starX = (width * ((i * 37) % 100 / 100f))
            val starY = (height * ((i * 53) % 100 / 100f))
            val alpha = (0.2f + 0.6f * sin(phase * 2f + i).coerceIn(0f, 1f))
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = if (i % 5 == 0) 2.5f else 1.5f,
                center = Offset(starX, starY)
            )
        }
    }
}

private fun getFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date())
}

private fun getFormattedDate(): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    return sdf.format(Date())
}
