package com.easeaudio.ui.screens

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.components.AudioVisualizerCanvas
import com.easeaudio.ui.components.VisualizerStyle
import com.easeaudio.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
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
    trackArtworkUrl: String? = null,
    onTogglePlay: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(getFormattedTime()) }
    var currentDate by remember { mutableStateOf(getFormattedDate()) }
    var selectedTheme by remember { mutableStateOf(AmbientTheme.STATION_ART) }
    var isDimmed by remember { mutableStateOf(false) }
    var showOverlayControls by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val containerFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        containerFocusRequester.requestFocus()
    }

    LaunchedEffect(showOverlayControls) {
        if (showOverlayControls) {
            delay(120)
            try {
                playPauseFocusRequester.requestFocus()
            } catch (_: Exception) {}
        } else {
            try {
                containerFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(showOverlayControls, lastInteractionTime) {
        if (showOverlayControls) {
            delay(8000L)
            if (System.currentTimeMillis() - lastInteractionTime >= 8000L) {
                showOverlayControls = false
            }
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
            .background(MaterialTheme.colorScheme.background)
            .focusRequester(containerFocusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                lastInteractionTime = System.currentTimeMillis()
                if (keyEvent.type == KeyEventType.KeyDown) {
                    if (keyEvent.key == Key.Back || keyEvent.key == Key.Escape) {
                        if (showOverlayControls) {
                            showOverlayControls = false
                            return@onKeyEvent true
                        }
                    } else if (!showOverlayControls) {
                        showOverlayControls = true
                        return@onKeyEvent true
                    }
                }
                false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                lastInteractionTime = System.currentTimeMillis()
                showOverlayControls = !showOverlayControls
            }
    ) {
        val maxScreenHeight = maxHeight
        val isCompactHeight = maxScreenHeight < 680.dp

        // LAYER 1: Full Screen Background Image / Ambient Theme
        when (selectedTheme) {
            AmbientTheme.STATION_ART -> {
                val effectiveArt = trackArtworkUrl?.ifBlank { null } ?: currentStation?.imageUrl
                if (effectiveArt?.isNotEmpty() == true) {
                    AsyncImage(
                        model = effectiveArt,
                        contentDescription = "Background Cover Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(if (isDimmed) 14.dp else 8.dp)
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

        val dimAlpha = if (isDimmed) 0.88f else 0.58f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = dimAlpha + 0.1f),
                            Color.Black.copy(alpha = dimAlpha - 0.15f),
                            Color.Black.copy(alpha = dimAlpha + 0.28f)
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
                    vertical = if (isCompactHeight) 10.dp else 18.dp
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
                    var isSleepPillFocused by remember { mutableStateOf(false) }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            width = if (isSleepPillFocused) 2.dp else 1.dp,
                            color = if (isSleepPillFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .onFocusChanged {
                                isSleepPillFocused = it.isFocused
                                if (it.isFocused) lastInteractionTime = System.currentTimeMillis()
                            }
                            .scale(if (isSleepPillFocused) 1.08f else 1.0f)
                            .clickable {
                                lastInteractionTime = System.currentTimeMillis()
                                onOpenSleepTimer()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bedtime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.sleeping_in, sleepTimerRemaining),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                var isDimFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = {
                        lastInteractionTime = System.currentTimeMillis()
                        isDimmed = !isDimmed
                    },
                    modifier = Modifier
                        .onFocusChanged {
                            isDimFocused = it.isFocused
                            if (it.isFocused) lastInteractionTime = System.currentTimeMillis()
                        }
                        .scale(if (isDimFocused) 1.15f else 1.0f)
                        .clip(CircleShape)
                        .background(if (isDimFocused) MaterialTheme.colorScheme.surfaceVariant else Color.Black.copy(alpha = 0.4f))
                        .border(
                            width = if (isDimFocused) 2.5.dp else 0.dp,
                            color = if (isDimFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("btn_ambient_dim")
                ) {
                    Icon(
                        imageVector = if (isDimmed) Icons.Filled.LightMode else Icons.Outlined.BrightnessMedium,
                        contentDescription = stringResource(R.string.night_dim),
                        tint = if (isDimmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 12.dp))

            // Center Column: Clock & Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = if (isCompactHeight) 4.dp else 10.dp)
            ) {
                Text(
                    text = currentTime,
                    fontSize = if (isCompactHeight) 46.sp else 64.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDimmed) 0.7f else 0.95f),
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.displayLarge
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = currentDate,
                    style = if (isCompactHeight) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (isDimmed) 0.6f else 0.85f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 12.dp))

            // Bottom Area: Seamless Mutually-Exclusive View (No Overlap!)
            // View A: Static Ambient Info Card with Waveform
            AnimatedVisibility(
                visible = !showOverlayControls,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (currentStation != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = if (isDimmed) 0.5f else 0.8f),
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = streamTitle ?: currentStation.genre,
                                        style = if (isCompactHeight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    Text(
                                        text = currentStation.bitrate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Waveform Visualizer
                                AudioVisualizerCanvas(
                                    waveAmplitudes = waveAmplitudes,
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .height(30.dp)
                                        .fillMaxWidth(0.55f),
                                    style = VisualizerStyle.ROUNDED_BARS,
                                    primaryColor = MaterialTheme.colorScheme.primary,
                                    secondaryColor = MaterialTheme.colorScheme.secondary,
                                    accentColor = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tap_tuner_pick_station),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.tap_for_ambient_controls),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // View B: Interactive Ambient Controls Bar with Full D-pad Focus Support
            AnimatedVisibility(
                visible = showOverlayControls,
                enter = fadeIn(animationSpec = tween(250)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Surface(
                    color = Color(0xFF101216).copy(alpha = 0.95f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Station Header info inside control bar
                        if (currentStation != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentStation.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(" • ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                Text(
                                    text = streamTitle ?: currentStation.genre,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Theme Selector Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Wallpaper,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(end = 2.dp)
                            )

                            AmbientTheme.entries.forEach { theme ->
                                val isSelected = selectedTheme == theme
                                var isChipFocused by remember { mutableStateOf(false) }
                                val chipScale by animateFloatAsState(
                                    targetValue = if (isChipFocused) 1.08f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    label = "chip_focus"
                                )

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isChipFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(
                                        width = if (isChipFocused) 2.5.dp else if (isSelected) 1.5.dp else 1.dp,
                                        brush = if (isChipFocused) {
                                            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, Color.White, MaterialTheme.colorScheme.primary))
                                        } else if (isSelected) {
                                            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
                                        } else {
                                            Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
                                        }
                                    ),
                                    modifier = Modifier
                                        .onFocusChanged {
                                            isChipFocused = it.isFocused
                                            if (it.isFocused) lastInteractionTime = System.currentTimeMillis()
                                        }
                                        .scale(chipScale)
                                        .shadow(
                                            elevation = if (isChipFocused) 10.dp else 0.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            spotColor = MaterialTheme.colorScheme.primary
                                        )
                                        .clickable {
                                            selectedTheme = theme
                                            lastInteractionTime = System.currentTimeMillis()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.background,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = stringResource(theme.nameRes),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected || isChipFocused) FontWeight.Black else FontWeight.Medium
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.background else if (isChipFocused) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Transport Action Controls Row (D-pad optimized)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var isTimerFocused by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    lastInteractionTime = System.currentTimeMillis()
                                    onOpenSleepTimer()
                                },
                                modifier = Modifier
                                    .onFocusChanged {
                                        isTimerFocused = it.isFocused
                                        if (it.isFocused) lastInteractionTime = System.currentTimeMillis()
                                    }
                                    .scale(if (isTimerFocused) 1.15f else 1.0f)
                                    .shadow(
                                        elevation = if (isTimerFocused) 10.dp else 0.dp,
                                        shape = CircleShape,
                                        spotColor = MaterialTheme.colorScheme.primary
                                    )
                                    .clip(CircleShape)
                                    .background(if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primaryContainer else if (isTimerFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .border(
                                        width = if (isTimerFocused) 2.5.dp else 0.dp,
                                        color = if (isTimerFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .size(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Bedtime,
                                    contentDescription = stringResource(R.string.sleep_timer),
                                    tint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.onPrimaryContainer else if (isTimerFocused) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            var isPlayFocused by remember { mutableStateOf(false) }
                            val playScale by animateFloatAsState(
                                targetValue = if (isPlayFocused) 1.18f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "play_focus"
                            )

                            Box(
                                modifier = Modifier
                                    .focusRequester(playPauseFocusRequester)
                                    .onFocusChanged {
                                        isPlayFocused = it.isFocused
                                        if (it.isFocused) lastInteractionTime = System.currentTimeMillis()
                                    }
                                    .scale(playScale)
                                    .shadow(
                                        elevation = if (isPlayFocused) 16.dp else 4.dp,
                                        shape = CircleShape,
                                        spotColor = MaterialTheme.colorScheme.primary,
                                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    )
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(
                                        width = if (isPlayFocused) 3.5.dp else 0.dp,
                                        brush = if (isPlayFocused) {
                                            Brush.horizontalGradient(listOf(Color.White, MaterialTheme.colorScheme.primary, Color.White))
                                        } else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        lastInteractionTime = System.currentTimeMillis()
                                        onTogglePlay()
                                    }
                                    .size(54.dp)
                                    .testTag("dock_play_pause"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.background,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            if (onToggleFavorite != null && currentStation != null) {
                                var isFavFocused by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        lastInteractionTime = System.currentTimeMillis()
                                        onToggleFavorite()
                                    },
                                    modifier = Modifier
                                        .onFocusChanged {
                                            isFavFocused = it.isFocused
                                            if (it.isFocused) lastInteractionTime = System.currentTimeMillis()
                                        }
                                        .scale(if (isFavFocused) 1.15f else 1.0f)
                                        .shadow(
                                            elevation = if (isFavFocused) 10.dp else 0.dp,
                                            shape = CircleShape,
                                            spotColor = MaterialTheme.colorScheme.primary
                                        )
                                        .clip(CircleShape)
                                        .background(if (isFavFocused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .border(
                                            width = if (isFavFocused) 2.5.dp else 0.dp,
                                            color = if (isFavFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .size(46.dp)
                                ) {
                                    Icon(
                                        imageVector = if (currentStation.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (currentStation.isFavorite) FavoriteHeartColor else if (isFavFocused) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
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
                colors = listOf(secondaryColor.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(width * 0.3f + xOffset1, height * 0.35f + yOffset1),
                radius = width * 0.8f
            )
        )

        val xOffset2 = (sin(phase * 1.3f) * 180f)
        val yOffset2 = (sin(phase * 0.9f) * 120f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
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
