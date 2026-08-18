package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easeaudio.R
import com.easeaudio.data.PodcastEpisode
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.components.AudioVisualizerCanvas
import com.easeaudio.ui.components.VisualizerStyle
import com.easeaudio.ui.theme.FavoriteHeartColor
import com.easeaudio.viewmodel.EqPresetDisplay
import com.easeaudio.viewmodel.HomeUiState

enum class CarTab {
    Player, Radio, Podcast, Favorites
}

enum class SideListTab {
    Favorites, Recent, Episodes, Top
}

@Composable
fun CarModeScreen(
    uiState: HomeUiState,
    onPlayPause: () -> Unit,
    onNextStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onSelectStation: (RadioStation) -> Unit,
    onEpisodeSelect: (RadioStation, PodcastEpisode) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit = {},
    onTabSelect: (HomeTab) -> Unit,
    onGenreSelect: (String) -> Unit,
    onCountrySelect: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onSeekRelative: ((Long) -> Unit)? = null,
    onSeek: ((Long) -> Unit)? = null,
    onPlaybackSpeedChange: ((Float) -> Unit)? = null,
    onOpenSleepTimer: (() -> Unit)? = null,
    onOpenEqualizer: (() -> Unit)? = null,
    onOpenEpisodes: (() -> Unit)? = null,
    eqPresets: List<EqPresetDisplay> = emptyList(),
    onExitCarMode: () -> Unit
) {
    val hasLastPlayed = remember {
        uiState.isPlaying || (uiState.currentStation != null && uiState.currentStation.lastListenedTimestamp > 0)
    }
    var activeCarTab by remember { mutableStateOf(if (hasLastPlayed) CarTab.Player else CarTab.Radio) }
    var showCountryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (activeCarTab == CarTab.Radio) {
            onTabSelect(HomeTab.Radio)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 4.dp, end = 6.dp)
    ) {
        val isWide = this.maxWidth > 640.dp
        val availableHeight = this.maxHeight

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // SIDEBAR NAVIGATION for Widescreen Automotive Displays
                    if (isWide) {
                        CarSideNav(
                            selectedTab = activeCarTab,
                            onTabSelect = { tab ->
                                activeCarTab = tab
                                if (tab == CarTab.Radio) onTabSelect(HomeTab.Radio)
                                if (tab == CarTab.Podcast) onTabSelect(HomeTab.Podcast)
                            },
                            onCountryClick = { showCountryDialog = true },
                            selectedCountry = uiState.selectedCountry,
                            onExit = onExitCarMode
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // TOP NAVIGATION for Portrait or Compact Displays
                        if (!isWide) {
                            CarTopNav(
                                selectedTab = activeCarTab,
                                onTabSelect = { tab ->
                                    activeCarTab = tab
                                    if (tab == CarTab.Radio) onTabSelect(HomeTab.Radio)
                                    if (tab == CarTab.Podcast) onTabSelect(HomeTab.Podcast)
                                },
                                onCountryClick = { showCountryDialog = true },
                                selectedCountry = uiState.selectedCountry,
                                onExit = onExitCarMode
                            )
                        }

                        // Main Content View
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(
                                    horizontal = if (isWide) 16.dp else 12.dp,
                                    vertical = if (isWide) 6.dp else 4.dp
                                )
                                .padding(bottom = if (activeCarTab != CarTab.Player && uiState.currentStation != null) 76.dp else 0.dp)
                        ) {
                            AnimatedContent(
                                targetState = activeCarTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "car_tab_transition"
                            ) { targetTab ->
                                when (targetTab) {
                                    CarTab.Player -> {
                                        AutomotiveHeroPlayer(
                                            uiState = uiState,
                                            onPlayPause = onPlayPause,
                                            onNextStation = onNextStation,
                                            onPreviousStation = onPreviousStation,
                                            onSelectStation = onSelectStation,
                                            onEpisodeSelect = onEpisodeSelect,
                                            onToggleFavorite = onToggleFavorite,
                                            onSeekRelative = onSeekRelative,
                                            onSeek = onSeek,
                                            onPlaybackSpeedChange = onPlaybackSpeedChange,
                                            onOpenSleepTimer = onOpenSleepTimer,
                                            onOpenEqualizer = onOpenEqualizer,
                                            onOpenEpisodes = onOpenEpisodes,
                                            eqPresets = eqPresets,
                                            isWide = isWide,
                                            availableHeight = availableHeight,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    CarTab.Radio, CarTab.Podcast -> {
                                        AutomotiveDiscoveryPanel(
                                            uiState = uiState,
                                            onGenreSelect = onGenreSelect,
                                            onCountryPickerOpen = { showCountryDialog = true },
                                            onSearchQueryChange = onSearchQueryChange,
                                            onStationSelect = {
                                                onSelectStation(it)
                                                activeCarTab = CarTab.Player
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            onLoadMore = onLoadMore,
                                            columns = if (isWide) 2 else 1
                                        )
                                    }
                                    CarTab.Favorites -> {
                                        AutomotiveFavoritesPanel(
                                            favoriteStations = uiState.favoriteStations,
                                            currentStationId = uiState.currentStation?.id,
                                            onStationSelect = {
                                                onSelectStation(it)
                                                activeCarTab = CarTab.Player
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            columns = if (isWide) 2 else 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // MINI PLAYER - Pinned to bottom when browsing lists
                if (activeCarTab != CarTab.Player && uiState.currentStation != null) {
                    CarMiniPlayer(
                        station = uiState.currentStation!!,
                        isPlaying = uiState.isPlaying,
                        isLoading = uiState.isLoading,
                        streamTitle = uiState.streamTitle,
                        waveAmplitudes = uiState.waveAmplitudes,
                        currentPosition = uiState.currentPlaybackPosition,
                        totalDuration = uiState.totalPlaybackDuration,
                        onTogglePlay = onPlayPause,
                        onNext = onNextStation,
                        onClick = { activeCarTab = CarTab.Player },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = if (isWide) 24.dp else 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showCountryDialog) {
        com.easeaudio.ui.components.CountrySelectionDialog(
            selectedCountry = uiState.selectedCountry,
            countries = uiState.availableCountries,
            onSelectCountry = { country ->
                onCountrySelect(country)
                showCountryDialog = false
            },
            onDismiss = { showCountryDialog = false }
        )
    }
}

@Composable
private fun CarSideNav(
    selectedTab: CarTab,
    onTabSelect: (CarTab) -> Unit,
    onCountryClick: () -> Unit,
    selectedCountry: String,
    onExit: () -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit Automotive Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        },
        modifier = Modifier
            .width(88.dp)
            .fillMaxHeight()
    ) {
        val items = listOf(
            Triple(CarTab.Player, Icons.Filled.PlayCircle, "Player"),
            Triple(CarTab.Radio, Icons.Filled.Radio, "Radio"),
            Triple(CarTab.Podcast, Icons.Filled.Mic, "Podcasts"),
            Triple(CarTab.Favorites, Icons.Filled.Favorite, "Saved")
        )

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items.forEach { (tab, icon, label) ->
                    val isSelected = selectedTab == tab
                    NavigationRailItem(
                        selected = isSelected,
                        onClick = { onTabSelect(tab) },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                modifier = Modifier.size(26.dp)
                            )
                        },
                        label = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Bottom Country Shortcut
            IconButton(
                onClick = onCountryClick,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = "Select Country ($selectedCountry)",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CarTopNav(
    selectedTab: CarTab,
    onTabSelect: (CarTab) -> Unit,
    onCountryClick: () -> Unit,
    selectedCountry: String,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = Color.Transparent,
            divider = {},
            indicator = {},
            edgePadding = 0.dp,
            modifier = Modifier.weight(1f)
        ) {
            CarTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val label = when (tab) {
                    CarTab.Player -> "Now Playing"
                    CarTab.Radio -> "Live Radio"
                    CarTab.Podcast -> "Podcasts"
                    CarTab.Favorites -> "Favorites"
                }
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelect(tab) },
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onCountryClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Country",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Exit",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Highly optimized, adaptive Automotive Hero Player.
 * Fits both wide automotive displays and compact screens without clipping or overflow.
 */
@Composable
private fun AutomotiveHeroPlayer(
    uiState: HomeUiState,
    onPlayPause: () -> Unit,
    onNextStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onSelectStation: (RadioStation) -> Unit,
    onEpisodeSelect: (RadioStation, PodcastEpisode) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onSeekRelative: ((Long) -> Unit)?,
    onSeek: ((Long) -> Unit)?,
    onPlaybackSpeedChange: ((Float) -> Unit)?,
    onOpenSleepTimer: (() -> Unit)?,
    onOpenEqualizer: (() -> Unit)?,
    onOpenEpisodes: (() -> Unit)?,
    eqPresets: List<EqPresetDisplay>,
    isWide: Boolean,
    availableHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val currentStation = uiState.currentStation
    val isPlaying = uiState.isPlaying
    val isLoading = uiState.isLoading
    val isPodcast = currentStation?.isPodcast == true
    val haptic = LocalHapticFeedback.current

    var activeSideTab by remember { mutableStateOf(if (isPodcast) SideListTab.Episodes else SideListTab.Favorites) }
    var carVisualizerStyle by remember { mutableStateOf(VisualizerStyle.DUAL_MIRROR) }

    // Auto switch side tab to episodes when playing a podcast
    LaunchedEffect(isPodcast) {
        if (isPodcast) {
            activeSideTab = SideListTab.Episodes
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        if (isWide) {
            // Widescreen Automotive Layout: Left (Controls/Hero) & Right (Supporting Quick List)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT HERO PANEL
                Column(
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Section: Artwork + Station/Podcast Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Artwork with glowing border and 1-tap heart
                        val artSize = if (availableHeight < 400.dp) 92.dp else 108.dp
                        Box(
                            modifier = Modifier
                                .size(artSize)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .border(
                                    1.dp,
                                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentStation?.imageUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = currentStation.imageUrl,
                                    contentDescription = currentStation.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPodcast) Icons.Filled.Mic else Icons.Filled.Radio,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            }

                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(34.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            }

                            // 1-tap Favorite Heart on Artwork
                            if (currentStation != null) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleFavorite(currentStation)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(30.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (currentStation.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Favorite",
                                        tint = if (currentStation.isFavorite) FavoriteHeartColor else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Meta details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Badge pill row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        if (isPlaying) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                        }
                                        Text(
                                            text = if (isPodcast) "PODCAST" else (currentStation?.genre?.uppercase() ?: "LIVE RADIO"),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp
                                            ),
                                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!currentStation?.bitrate.isNullOrBlank() && !isPodcast) {
                                    Text(
                                        text = currentStation!!.bitrate,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }

                            // Station or Show Name
                            Text(
                                text = currentStation?.name ?: stringResource(R.string.no_station_selected),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Track Title / Episode Title
                            val secondaryText = if (isPodcast) {
                                uiState.currentEpisode?.title ?: currentStation?.genre ?: ""
                            } else {
                                if (isLoading) "Buffering stream..." else (uiState.streamTitle ?: currentStation?.country ?: "")
                            }

                            if (secondaryText.isNotBlank()) {
                                Text(
                                    text = secondaryText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    // Middle Section: Scrubber for Podcast, or Waveform Visualizer for Radio
                    if (isPodcast) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Slider(
                                value = uiState.currentPlaybackPosition.toFloat().coerceIn(0f, (uiState.totalPlaybackDuration.takeIf { it > 0 } ?: 1L).toFloat()),
                                onValueChange = { pos ->
                                    onSeek?.invoke(pos.toLong())
                                },
                                valueRange = 0f..(uiState.totalPlaybackDuration.takeIf { it > 0 } ?: 1L).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(uiState.currentPlaybackPosition),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(uiState.totalPlaybackDuration),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Full-Width Automotive Audio Spectrum & Visualizer Centerpiece
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (availableHeight < 400.dp) 70.dp else 88.dp)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    carVisualizerStyle = when (carVisualizerStyle) {
                                        VisualizerStyle.DUAL_MIRROR -> VisualizerStyle.ROUNDED_BARS
                                        VisualizerStyle.ROUNDED_BARS -> VisualizerStyle.NEON_RIBBON
                                        VisualizerStyle.NEON_RIBBON -> VisualizerStyle.WAVE_LINE
                                        VisualizerStyle.WAVE_LINE -> VisualizerStyle.DOT_MATRIX
                                        else -> VisualizerStyle.DUAL_MIRROR
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                1.dp,
                                if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top status bar inside spectrum card
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .background(
                                                    if (isPlaying) MaterialTheme.colorScheme.primary
                                                    else if (isLoading) Color(0xFFFFB300)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (isLoading) "CONNECTING STREAM..." else if (isPlaying) "AUDIO SPECTRUM" else "STREAM READY",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Right pill showing format specs & active style
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (currentStation != null) {
                                            Text(
                                                text = "${currentStation.codec} • ${currentStation.bitrate}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.GraphicEq,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(11.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                val styleLabel = when (carVisualizerStyle) {
                                                    VisualizerStyle.ROUNDED_BARS -> "Bars"
                                                    VisualizerStyle.DUAL_MIRROR -> "Mirror"
                                                    VisualizerStyle.NEON_RIBBON -> "Neon"
                                                    VisualizerStyle.WAVE_LINE -> "Wave"
                                                    VisualizerStyle.DOT_MATRIX -> "LED"
                                                    else -> "EQ"
                                                }
                                                Text(
                                                    text = styleLabel,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }

                                // Full-width dynamic audio visualizer canvas
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(top = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AudioVisualizerCanvas(
                                        waveAmplitudes = uiState.waveAmplitudes.ifEmpty { List(28) { 0.15f } },
                                        isPlaying = isPlaying,
                                        modifier = Modifier.fillMaxSize(),
                                        style = carVisualizerStyle,
                                        primaryColor = MaterialTheme.colorScheme.primary,
                                        secondaryColor = MaterialTheme.colorScheme.secondary,
                                        accentColor = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }

                    // Transport Controls Row (Large & High Contrast for Driving)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPreviousStation()
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        if (isPodcast && onSeekRelative != null) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSeekRelative(-10000L)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Big Play / Pause Button
                        FilledIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayPause()
                            },
                            modifier = Modifier
                                .size(68.dp)
                                .border(
                                    width = if (isPlaying) 2.5.dp else 0.dp,
                                    color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        if (isPodcast && onSeekRelative != null) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSeekRelative(30000L)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Forward30,
                                    contentDescription = "Forward 30s",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNextStation()
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Bottom Quick Action Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPodcast && onPlaybackSpeedChange != null) {
                            AssistChip(
                                onClick = {
                                    val nextSpeed = when (uiState.playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                    onPlaybackSpeedChange(nextSpeed)
                                },
                                label = { Text("${uiState.playbackSpeed}x") },
                                leadingIcon = { Icon(Icons.Filled.Speed, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            )
                        }

                        if (isPodcast && onOpenEpisodes != null) {
                            AssistChip(
                                onClick = onOpenEpisodes,
                                label = { Text("Episodes") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            )
                        }

                        if (!isPodcast && onOpenEqualizer != null) {
                            val activePresetLabel = eqPresets.find { it.key == uiState.activeEqPreset }?.labelResId?.let { stringResource(it) } ?: uiState.activeEqPreset
                            AssistChip(
                                onClick = onOpenEqualizer,
                                label = { Text("EQ: $activePresetLabel") },
                                leadingIcon = { Icon(Icons.Filled.Tune, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            )
                        }

                        if (onOpenSleepTimer != null) {
                            val isTimerActive = uiState.sleepTimerRemaining != null
                            AssistChip(
                                onClick = onOpenSleepTimer,
                                label = { Text(if (isTimerActive) "${uiState.sleepTimerRemaining}m" else "Sleep Timer") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Bedtime,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isTimerActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = if (isTimerActive) AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else AssistChipDefaults.assistChipColors()
                            )
                        }
                    }
                }

                // RIGHT SUPPORTING SIDE PANE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(8.dp)
                ) {
                    // Pane Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val sideTabs = if (isPodcast) {
                            listOf(SideListTab.Episodes to "Episodes", SideListTab.Favorites to "Saved", SideListTab.Recent to "Recents")
                        } else {
                            listOf(SideListTab.Favorites to "Favorites", SideListTab.Recent to "Recents", SideListTab.Top to "Explore")
                        }

                        sideTabs.forEach { (tab, label) ->
                            val isSelected = activeSideTab == tab
                            Surface(
                                onClick = { activeSideTab = tab },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Content of selected side tab
                    when (activeSideTab) {
                        SideListTab.Episodes -> {
                            if (uiState.isLoadingEpisodes) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            } else if (uiState.currentEpisodesList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Mic,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "No episodes loaded",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.currentEpisodesList) { episode ->
                                        val isCurrent = uiState.currentEpisode?.id == episode.id
                                        Surface(
                                            onClick = { currentStation?.let { onEpisodeSelect(it, episode) } },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                            border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = episode.artworkUrl.ifBlank { currentStation?.imageUrl },
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = episode.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium),
                                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = episode.pubDate,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SideListTab.Favorites -> {
                            if (uiState.favoriteStations.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FavoriteBorder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "No saved favorites yet",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { activeSideTab = if (isPodcast) SideListTab.Episodes else SideListTab.Top },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (isPodcast) "View Episodes" else "Browse Stations",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            } else {
                                AutomotiveStationList(
                                    stations = uiState.favoriteStations,
                                    currentStationId = currentStation?.id,
                                    onSelectStation = onSelectStation,
                                    onToggleFavorite = onToggleFavorite,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        SideListTab.Recent -> {
                            val recents = if (isPodcast) uiState.recentPodcastStations else uiState.recentRadioStations
                            if (recents.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            text = "No recent stations",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = { activeSideTab = SideListTab.Top },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Browse Stations",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            } else {
                                AutomotiveStationList(
                                    stations = recents,
                                    currentStationId = currentStation?.id,
                                    onSelectStation = onSelectStation,
                                    onToggleFavorite = onToggleFavorite,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        SideListTab.Top -> {
                            AutomotiveStationList(
                                stations = uiState.stations.take(20),
                                currentStationId = currentStation?.id,
                                onSelectStation = onSelectStation,
                                onToggleFavorite = onToggleFavorite,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait / Compact Automotive Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Artwork
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentStation?.imageUrl,
                        contentDescription = currentStation?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                    }
                    if (currentStation != null) {
                        IconButton(
                            onClick = { onToggleFavorite(currentStation) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (currentStation.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentStation.isFavorite) FavoriteHeartColor else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Station & Track Meta
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentStation?.name ?: stringResource(R.string.no_station_selected),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPodcast) (uiState.currentEpisode?.title ?: currentStation?.genre ?: "") else (uiState.streamTitle ?: currentStation?.genre ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Visualizer Card
                if (!isPodcast) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                carVisualizerStyle = when (carVisualizerStyle) {
                                    VisualizerStyle.DUAL_MIRROR -> VisualizerStyle.ROUNDED_BARS
                                    VisualizerStyle.ROUNDED_BARS -> VisualizerStyle.NEON_RIBBON
                                    VisualizerStyle.NEON_RIBBON -> VisualizerStyle.WAVE_LINE
                                    VisualizerStyle.WAVE_LINE -> VisualizerStyle.DOT_MATRIX
                                    else -> VisualizerStyle.DUAL_MIRROR
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AudioVisualizerCanvas(
                                waveAmplitudes = uiState.waveAmplitudes.ifEmpty { List(24) { 0.15f } },
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxSize(),
                                style = carVisualizerStyle,
                                primaryColor = MaterialTheme.colorScheme.primary,
                                secondaryColor = MaterialTheme.colorScheme.secondary,
                                accentColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Podcast Slider
                if (isPodcast) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = uiState.currentPlaybackPosition.toFloat().coerceIn(0f, (uiState.totalPlaybackDuration.takeIf { it > 0 } ?: 1L).toFloat()),
                            onValueChange = { pos -> onSeek?.invoke(pos.toLong()) },
                            valueRange = 0f..(uiState.totalPlaybackDuration.takeIf { it > 0 } ?: 1L).toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDuration(uiState.currentPlaybackPosition), style = MaterialTheme.typography.labelSmall)
                            Text(formatDuration(uiState.totalPlaybackDuration), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousStation,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.SkipPrevious, null, modifier = Modifier.size(28.dp))
                    }

                    if (isPodcast && onSeekRelative != null) {
                        IconButton(onClick = { onSeekRelative(-10000L) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Replay10, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(68.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    if (isPodcast && onSeekRelative != null) {
                        IconButton(onClick = { onSeekRelative(30000L) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Forward30, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(
                        onClick = onNextStation,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomotiveStationList(
    stations: List<RadioStation>,
    currentStationId: String?,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(stations, key = { it.id }) { station ->
            val isCurrent = currentStationId == station.id
            Surface(
                onClick = { onSelectStation(station) },
                shape = RoundedCornerShape(10.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = station.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = station.genre,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite(station)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomotiveDiscoveryPanel(
    uiState: HomeUiState,
    onGenreSelect: (String) -> Unit,
    onCountryPickerOpen: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onLoadMore: () -> Unit,
    columns: Int
) {
    val isPodcastTab = uiState.selectedTab == HomeTab.Podcast
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        if (isPodcastTab) "Search podcasts & shows..." else "Search radio stations...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            // Country Selector Button
            Button(
                onClick = onCountryPickerOpen,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Filled.Language, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = uiState.selectedCountry,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
        }

        // Category/Genre Chips Horizontal Row
        val genres = if (isPodcastTab) uiState.availablePodcastTopics else uiState.availableGenres
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            items(genres) { genre ->
                val isSelected = uiState.selectedGenre == genre.key
                Surface(
                    onClick = { onGenreSelect(genre.key) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(genre.labelResId),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Station Grid
        AutomotiveGrid(
            stations = uiState.stations,
            currentStationId = uiState.currentStation?.id,
            isPlaying = uiState.isPlaying,
            onSelectStation = onStationSelect,
            onToggleFavorite = onToggleFavorite,
            onLoadMore = onLoadMore,
            columns = columns,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AutomotiveFavoritesPanel(
    favoriteStations: List<RadioStation>,
    currentStationId: String?,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    columns: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Saved Favorites",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (favoriteStations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No saved stations. Tap the heart icon on any station to save it here for fast access while driving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            AutomotiveGrid(
                stations = favoriteStations,
                currentStationId = currentStationId,
                isPlaying = false,
                onSelectStation = onStationSelect,
                onToggleFavorite = onToggleFavorite,
                columns = columns,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AutomotiveGrid(
    stations: List<RadioStation>,
    currentStationId: String?,
    isPlaying: Boolean = false,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onLoadMore: () -> Unit = {},
    columns: Int,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val shouldLoadMore = remember(gridState) {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) onLoadMore()
    }

    val haptic = LocalHapticFeedback.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        items(stations, key = { it.id }) { station ->
            val isSelected = currentStationId == station.id
            var isCardFocused by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .onFocusChanged { isCardFocused = it.isFocused }
                    .border(
                        width = if (isCardFocused) 2.dp else if (isSelected) 1.5.dp else 0.dp,
                        color = if (isCardFocused || isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectStation(station) },
                color = if (isCardFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surface,
                tonalElevation = if (isSelected) 4.dp else 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = station.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isSelected && isPlaying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected || isCardFocused) FontWeight.Black else FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = if (isSelected || isCardFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = station.genre.ifBlank { station.country },
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (station.country.isNotBlank() && station.genre.isNotBlank()) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = station.country,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite(station)
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (station.isFavorite) FavoriteHeartColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CarMiniPlayer(
    station: RadioStation,
    isPlaying: Boolean,
    isLoading: Boolean,
    streamTitle: String?,
    waveAmplitudes: List<Float>,
    currentPosition: Long,
    totalDuration: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = station.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPlaying) {
                        AudioVisualizerCanvas(
                            waveAmplitudes = waveAmplitudes.take(8),
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .width(24.dp)
                                .height(10.dp),
                            style = VisualizerStyle.ROUNDED_BARS,
                            primaryColor = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isLoading) "Buffering..." else (streamTitle ?: station.genre),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Next button
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNext()
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next Station",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Play/Pause button
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePlay()
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        val remMinutes = minutes % 60
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
