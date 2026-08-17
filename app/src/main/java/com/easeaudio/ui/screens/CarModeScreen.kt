package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import com.easeaudio.ui.components.AudioVisualizerCanvas
import com.easeaudio.ui.components.VisualizerStyle

@Composable
fun CarModeScreen(
    currentStation: RadioStation?,
    isPlaying: Boolean,
    waveAmplitudes: List<Float> = List(8) { 0.2f },
    stations: List<RadioStation>,
    onPlayPause: () -> Unit,
    onNextStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit = {},
    onExitCarMode: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.car_mode_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 22.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                var isExitFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onExitCarMode,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isExitFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier
                        .onFocusChanged { isExitFocused = it.isFocused }
                        .border(
                            width = if (isExitFocused) 2.dp else 0.dp,
                            color = if (isExitFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = if (isExitFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = stringResource(R.string.exit_car_mode), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (isExitFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // Current Station Display Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentStation?.name ?: stringResource(R.string.no_station_selected),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentStation?.genre ?: "Radio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        if (currentStation != null && isPlaying) {
                            Spacer(modifier = Modifier.height(10.dp))
                            AudioVisualizerCanvas(
                                waveAmplitudes = waveAmplitudes,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .height(28.dp),
                                style = VisualizerStyle.ROUNDED_BARS
                            )
                        }
                    }

                    if (currentStation != null) {
                        var isFavFocused by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { onToggleFavorite(currentStation) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(48.dp)
                                .onFocusChanged { isFavFocused = it.isFocused }
                                .border(
                                    width = if (isFavFocused) 2.dp else 0.dp,
                                    color = if (isFavFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (currentStation.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentStation.isFavorite) FavoriteHeartColor else if (isFavFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Giant Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Button
                var isPrevFocused by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .onFocusChanged { isPrevFocused = it.isFocused }
                        .border(
                            width = if (isPrevFocused) 3.dp else 0.dp,
                            color = if (isPrevFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(onClick = onPreviousStation),
                    color = if (isPrevFocused) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = if (isPrevFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(36.dp))
                    }
                }

                // Main Play/Pause Giant Button
                var isPlayFocused by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .onFocusChanged { isPlayFocused = it.isFocused }
                        .border(
                            width = if (isPlayFocused) 4.dp else 0.dp,
                            color = if (isPlayFocused) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(onClick = onPlayPause),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Next Button
                var isNextFocused by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .onFocusChanged { isNextFocused = it.isFocused }
                        .border(
                            width = if (isNextFocused) 3.dp else 0.dp,
                            color = if (isNextFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(onClick = onNextStation),
                    color = if (isNextFocused) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next", tint = if (isNextFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(36.dp))
                    }
                }
            }

            // Large Touch / D-pad Station Cards Grid (Quick Pick)
            Text(
                text = stringResource(R.string.quick_select),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                items(stations.take(6)) { station ->
                    val isSelected = currentStation?.id == station.id
                    var isCardFocused by remember { mutableStateOf(false) }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .onFocusChanged { isCardFocused = it.isFocused }
                            .border(
                                width = if (isCardFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                                color = if (isCardFocused) MaterialTheme.colorScheme.primary else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectStation(station) },
                        color = if (isCardFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected || isCardFocused) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected || isCardFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

