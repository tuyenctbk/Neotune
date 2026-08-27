package com.easeaudio.ui.screens

import android.content.Context
import android.media.AudioManager
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.easeaudio.R
import com.easeaudio.data.CuratedStationsService
import com.easeaudio.data.PodcastEpisode
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.components.AudioVisualizerCanvas
import com.easeaudio.ui.components.VisualizerStyle
import com.easeaudio.ui.theme.FavoriteHeartColor
import com.easeaudio.viewmodel.EqPresetDisplay
import com.easeaudio.viewmodel.HomeUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CarTab {
    Player, Radio, Podcast, Favorites
}

enum class SideListTab {
    Favorites, Recent, Episodes, Top
}

object CarPresetsStore {
    private const val PREFS_NAME = "neotune_car_presets"
    private const val KEY_PRESET_PREFIX = "preset_slot_"

    fun loadPresets(context: Context, favorites: List<RadioStation>): List<RadioStation> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaultList = (favorites + CuratedStationsService.defaultCuratedStations).distinctBy { it.id }.take(6)
        val result = mutableListOf<RadioStation>()
        for (i in 0 until 6) {
            val json = prefs.getString("$KEY_PRESET_PREFIX$i", null)
            val station = if (json != null) {
                try {
                    val obj = org.json.JSONObject(json)
                    RadioStation(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        genre = obj.optString("genre"),
                        country = obj.optString("country"),
                        streamUrl = obj.getString("streamUrl"),
                        imageUrl = obj.optString("imageUrl"),
                        bitrate = obj.optString("bitrate"),
                        codec = obj.optString("codec")
                    )
                } catch (e: Exception) {
                    defaultList.getOrNull(i)
                }
            } else {
                defaultList.getOrNull(i)
            }
            if (station != null) {
                result.add(station)
            }
        }
        return result
    }

    fun savePreset(context: Context, slotIndex: Int, station: RadioStation) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val obj = org.json.JSONObject().apply {
            put("id", station.id)
            put("name", station.name)
            put("genre", station.genre)
            put("country", station.country)
            put("streamUrl", station.streamUrl)
            put("imageUrl", station.imageUrl)
            put("bitrate", station.bitrate)
            put("codec", station.codec)
        }
        prefs.edit().putString("$KEY_PRESET_PREFIX$slotIndex", obj.toString()).apply()
    }
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
    val context = LocalContext.current
    val hasLastPlayed = remember {
        uiState.isPlaying || (uiState.currentStation != null && uiState.currentStation.lastListenedTimestamp > 0)
    }
    var activeCarTab by remember { mutableStateOf(if (hasLastPlayed) CarTab.Player else CarTab.Radio) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var isAntiGlare by rememberSaveable { mutableStateOf(false) }

    // Live In-Car Digital Cockpit Clock
    var currentClockTime by remember { mutableStateOf("") }
    var currentClockDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            currentClockTime = timeFormat.format(now)
            currentClockDate = dateFormat.format(now)
            delay(10000L)
        }
    }

    LaunchedEffect(Unit) {
        if (activeCarTab == CarTab.Radio) {
            onTabSelect(HomeTab.Radio)
        }
    }

    // Car Presets state
    var carPresets by remember {
        mutableStateOf(CarPresetsStore.loadPresets(context, uiState.favoriteStations))
    }

    // Update presets when favorites change if empty
    LaunchedEffect(uiState.favoriteStations) {
        if (carPresets.isEmpty()) {
            carPresets = CarPresetsStore.loadPresets(context, uiState.favoriteStations)
        }
    }

    val backgroundColor = if (isAntiGlare) Color(0xFF000000) else MaterialTheme.colorScheme.background

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 4.dp, end = 6.dp)
    ) {
        val isWide = this.maxWidth > 640.dp
        val availableHeight = this.maxHeight

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor
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
                            isAntiGlare = isAntiGlare,
                            onToggleAntiGlare = { isAntiGlare = !isAntiGlare },
                            onExit = onExitCarMode
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // TOP NAVIGATION / COCKPIT HEADER
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
                                isAntiGlare = isAntiGlare,
                                onToggleAntiGlare = { isAntiGlare = !isAntiGlare },
                                clockTime = currentClockTime,
                                onExit = onExitCarMode
                            )
                        } else {
                            // Widescreen In-Car Cockpit Top Status Bar
                            CarWidescreenHeader(
                                clockTime = currentClockTime,
                                clockDate = currentClockDate,
                                isAntiGlare = isAntiGlare,
                                onToggleAntiGlare = { isAntiGlare = !isAntiGlare },
                                currentStation = uiState.currentStation,
                                isPlaying = uiState.isPlaying,
                                isLoading = uiState.isLoading
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
                                            carPresets = carPresets,
                                            onSelectPreset = { station ->
                                                onSelectStation(station)
                                            },
                                            onSavePreset = { slotIndex, station ->
                                                CarPresetsStore.savePreset(context, slotIndex, station)
                                                carPresets = CarPresetsStore.loadPresets(context, uiState.favoriteStations)
                                                Toast.makeText(
                                                    context,
                                                    context.getString(R.string.car_preset_saved_format, slotIndex + 1),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            isWide = isWide,
                                            availableHeight = availableHeight,
                                            isAntiGlare = isAntiGlare,
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
                                            columns = if (isWide) 2 else 1,
                                            isAntiGlare = isAntiGlare
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
                                            columns = if (isWide) 2 else 1,
                                            isAntiGlare = isAntiGlare
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
                        isAntiGlare = isAntiGlare,
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
private fun CarWidescreenHeader(
    clockTime: String,
    clockDate: String,
    isAntiGlare: Boolean,
    onToggleAntiGlare: () -> Unit,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cockpit Live Clock
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = clockTime.ifBlank { "--:--" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (clockDate.isNotBlank()) {
                        Text(
                            text = "• $clockDate",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Live On Air Status Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isPlaying) MaterialTheme.colorScheme.primary
                                else if (isLoading) Color(0xFFFFB300)
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                    Text(
                        text = if (isLoading) stringResource(R.string.car_cockpit_buffering)
                               else if (isPlaying) stringResource(R.string.car_cockpit_live_on_air)
                               else stringResource(R.string.car_cockpit_stream_ready),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Night Anti-Glare Dimmer Toggle
        IconButton(
            onClick = onToggleAntiGlare,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isAntiGlare) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
        ) {
            Icon(
                imageVector = if (isAntiGlare) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = stringResource(R.string.car_anti_glare_night),
                tint = if (isAntiGlare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CarSideNav(
    selectedTab: CarTab,
    onTabSelect: (CarTab) -> Unit,
    onCountryClick: () -> Unit,
    selectedCountry: String,
    isAntiGlare: Boolean,
    onToggleAntiGlare: () -> Unit,
    onExit: () -> Unit
) {
    NavigationRail(
        containerColor = if (isAntiGlare) Color(0xFF080C10) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                        contentDescription = stringResource(R.string.exit_car_mode),
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
            Triple(CarTab.Player, Icons.Filled.PlayCircle, stringResource(R.string.now_playing)),
            Triple(CarTab.Radio, Icons.Filled.Radio, stringResource(R.string.nav_radio)),
            Triple(CarTab.Podcast, Icons.Filled.Mic, stringResource(R.string.nav_podcast)),
            Triple(CarTab.Favorites, Icons.Filled.Favorite, stringResource(R.string.favorites))
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
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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

            // Bottom Actions: Anti-Glare and Country Shortcut
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                IconButton(
                    onClick = onToggleAntiGlare,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isAntiGlare) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = if (isAntiGlare) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = stringResource(R.string.car_anti_glare_night),
                        tint = if (isAntiGlare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onCountryClick,
                    modifier = Modifier
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
}

@Composable
private fun CarTopNav(
    selectedTab: CarTab,
    onTabSelect: (CarTab) -> Unit,
    onCountryClick: () -> Unit,
    selectedCountry: String,
    isAntiGlare: Boolean,
    onToggleAntiGlare: () -> Unit,
    clockTime: String,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Digital Clock Pill in Top Bar for portrait
        if (clockTime.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = clockTime,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

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
                    CarTab.Player -> stringResource(R.string.now_playing)
                    CarTab.Radio -> stringResource(R.string.live_radio_stations)
                    CarTab.Podcast -> stringResource(R.string.nav_podcast)
                    CarTab.Favorites -> stringResource(R.string.favorites)
                }
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelect(tab) },
                    modifier = Modifier
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Anti-Glare HUD Mode Toggle
        IconButton(
            onClick = onToggleAntiGlare,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isAntiGlare) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
        ) {
            Icon(
                imageVector = if (isAntiGlare) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                contentDescription = stringResource(R.string.car_anti_glare_night),
                tint = if (isAntiGlare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onCountryClick,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Filled.Language,
                contentDescription = "Country",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(
            onClick = onExit,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.exit_car_mode),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
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
    carPresets: List<RadioStation>,
    onSelectPreset: (RadioStation) -> Unit,
    onSavePreset: (Int, RadioStation) -> Unit,
    isWide: Boolean,
    availableHeight: androidx.compose.ui.unit.Dp,
    isAntiGlare: Boolean,
    modifier: Modifier = Modifier
) {
    val currentStation = uiState.currentStation
    val isPlaying = uiState.isPlaying
    val isLoading = uiState.isLoading
    val isPodcast = currentStation?.isPodcast == true
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    var activeSideTab by remember { mutableStateOf(if (isPodcast) SideListTab.Episodes else SideListTab.Favorites) }
    var carVisualizerStyle by remember { mutableStateOf(VisualizerStyle.DUAL_MIRROR) }

    // Auto switch side tab to episodes when playing a podcast
    LaunchedEffect(isPodcast) {
        if (isPodcast) {
            activeSideTab = SideListTab.Episodes
        }
    }

    val cardBackground = if (isAntiGlare) Color(0xFF090D12) else MaterialTheme.colorScheme.surface
    val cardBorderColor = if (isAntiGlare) Color(0xFF1E3A4D) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, cardBorderColor)
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
                        val artSize = if (availableHeight < 400.dp) 88.dp else 104.dp
                        val carContext = LocalContext.current
                        val effectiveArtworkUrl = uiState.trackArtworkUrl?.ifBlank { null } ?: currentStation?.imageUrl
                        val carImageRequest = remember(effectiveArtworkUrl) {
                            ImageRequest.Builder(carContext)
                                .data(effectiveArtworkUrl)
                                .crossfade(true)
                                .build()
                        }
                        Box(
                            modifier = Modifier
                                .size(artSize)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                                .border(
                                    1.5.dp,
                                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (effectiveArtworkUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = carImageRequest,
                                    contentDescription = currentStation?.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPodcast) Icons.Filled.Mic else Icons.Filled.Radio,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }

                            if (isLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(34.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
                                    )
                                }
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
                                        contentDescription = stringResource(R.string.favorites),
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
                                            text = if (isPodcast) stringResource(R.string.badge_podcast) else (currentStation?.genre?.uppercase() ?: stringResource(R.string.live_radio_stations)),
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
                                if (isLoading) stringResource(R.string.buffering_stream) else (uiState.streamTitle ?: currentStation?.country ?: "")
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
                                .height(if (availableHeight < 400.dp) 64.dp else 78.dp)
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
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Status header inside spectrum
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
                                                .size(6.dp)
                                                .background(
                                                    if (isPlaying) MaterialTheme.colorScheme.primary
                                                    else if (isLoading) Color(0xFFFFB300)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = if (isLoading) stringResource(R.string.car_cockpit_buffering)
                                                   else if (isPlaying) stringResource(R.string.audio_frequency_visualizer)
                                                   else stringResource(R.string.car_cockpit_stream_ready),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 10.sp,
                                                letterSpacing = 0.8.sp
                                            ),
                                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (currentStation != null) {
                                        Text(
                                            text = "${currentStation.codec} • ${currentStation.bitrate}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                        )
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

                    // 1-Tap In-Car Quick Radio Presets Bar (P1 - P6)
                    AutomotivePresetsRow(
                        presets = carPresets,
                        currentStationId = currentStation?.id,
                        onSelectPreset = onSelectPreset,
                        onSavePreset = { slotIdx ->
                            currentStation?.let { onSavePreset(slotIdx, it) }
                        }
                    )

                    // Transport Controls Row (Large & High Contrast for Driving)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPreviousStation()
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        if (isPodcast && onSeekRelative != null) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSeekRelative(-10000L)
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Replay10,
                                    contentDescription = "Rewind 10s",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
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
                                .size(70.dp)
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
                                    modifier = Modifier.size(32.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(40.dp)
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
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Forward30,
                                    contentDescription = "Forward 30s",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNextStation()
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    // Bottom Driver Quick Action Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Driver Volume Controls (Mute / Down / Up)
                        if (audioManager != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Filled.VolumeDown, contentDescription = stringResource(R.string.car_volume_down), modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Filled.VolumeMute, contentDescription = stringResource(R.string.car_volume_mute), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Filled.VolumeUp, contentDescription = stringResource(R.string.car_volume_up), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

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
                                 label = { Text(stringResource(R.string.speed_format, uiState.playbackSpeed.toString())) },
                                leadingIcon = { Icon(Icons.Filled.Speed, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            )
                        }

                        if (isPodcast && onOpenEpisodes != null) {
                            AssistChip(
                                onClick = onOpenEpisodes,
                                label = { Text(stringResource(R.string.episodes_label)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            )
                        }

                        if (!isPodcast && onOpenEqualizer != null) {
                            val activePresetLabel = eqPresets.find { it.key == uiState.activeEqPreset }?.labelResId?.let { stringResource(it) } ?: uiState.activeEqPreset
                            AssistChip(
                                onClick = onOpenEqualizer,
                                label = { Text(stringResource(R.string.eq_label, activePresetLabel)) },
                                leadingIcon = { Icon(Icons.Filled.Tune, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            )
                        }

                        if (onOpenSleepTimer != null) {
                            val isTimerActive = uiState.sleepTimerRemaining != null
                            AssistChip(
                                onClick = onOpenSleepTimer,
                                label = { Text(if (isTimerActive) "${uiState.sleepTimerRemaining}m" else stringResource(R.string.timer_label)) },
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
                            listOf(SideListTab.Episodes to stringResource(R.string.episodes_label), SideListTab.Favorites to stringResource(R.string.favorites), SideListTab.Recent to stringResource(R.string.recent_streams))
                        } else {
                            listOf(SideListTab.Favorites to stringResource(R.string.favorites), SideListTab.Recent to stringResource(R.string.recent_streams), SideListTab.Top to stringResource(R.string.browse_stations))
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
                                            text = stringResource(R.string.no_episodes_loaded),
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
                                            text = stringResource(R.string.no_saved_favorites_car),
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
                                                text = if (isPodcast) stringResource(R.string.view_episodes) else stringResource(R.string.browse_stations),
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
                                            text = stringResource(R.string.no_recent_stations),
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
                                                text = stringResource(R.string.browse_stations),
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Artwork
                val effectiveArtworkUrl = uiState.trackArtworkUrl?.ifBlank { null } ?: currentStation?.imageUrl
                val carContext = LocalContext.current
                val carImageRequest = remember(effectiveArtworkUrl) {
                    ImageRequest.Builder(carContext)
                        .data(effectiveArtworkUrl)
                        .crossfade(true)
                        .build()
                }
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                        .border(
                            1.5.dp,
                            if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (effectiveArtworkUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = carImageRequest,
                            contentDescription = currentStation?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (isPodcast) Icons.Filled.Mic else Icons.Filled.Radio,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                    }
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                        }
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
                                contentDescription = stringResource(R.string.favorites),
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
                            .fillMaxWidth(0.92f)
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

                // 1-Tap Quick Radio Presets Bar (P1 - P6)
                AutomotivePresetsRow(
                    presets = carPresets,
                    currentStationId = currentStation?.id,
                    onSelectPreset = onSelectPreset,
                    onSavePreset = { slotIdx ->
                        currentStation?.let { onSavePreset(slotIdx, it) }
                    }
                )

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
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.SkipPrevious, null, modifier = Modifier.size(30.dp))
                    }

                    if (isPodcast && onSeekRelative != null) {
                        IconButton(onClick = { onSeekRelative(-10000L) }, modifier = Modifier.size(50.dp)) {
                            Icon(Icons.Filled.Replay10, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }
                    }

                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(70.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    if (isPodcast && onSeekRelative != null) {
                        IconButton(onClick = { onSeekRelative(30000L) }, modifier = Modifier.size(50.dp)) {
                            Icon(Icons.Filled.Forward30, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                        }
                    }

                    IconButton(
                        onClick = onNextStation,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
    }
}

/**
 * 1-Tap In-Car Radio Presets Bar (P1 - P6)
 * Real automotive radio behavior:
 * - 1 tap: tune to preset immediately
 * - Long press: save current playing station to that slot
 */
@Composable
private fun AutomotivePresetsRow(
    presets: List<RadioStation>,
    currentStationId: String?,
    onSelectPreset: (RadioStation) -> Unit,
    onSavePreset: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 6) {
            val station = presets.getOrNull(i)
            val isCurrent = station != null && station.id == currentStationId

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (station != null) onSelectPreset(station)
                    else onSavePreset(i)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = if (isCurrent) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "P${i + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        ),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (station != null) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                            contentDescription = stringResource(R.string.favorites),
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
    columns: Int,
    isAntiGlare: Boolean
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
                        if (isPodcastTab) stringResource(R.string.search_results_podcasts) else stringResource(R.string.search_placeholder),
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
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search), modifier = Modifier.size(20.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = if (isAntiGlare) Color(0xFF090D12) else MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = if (isAntiGlare) Color(0xFF090D12) else MaterialTheme.colorScheme.surface
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

        // Curated Audiophile Feeds (Radio Paradise & SomaFM) in Live Radio mode
        if (!isPodcastTab && uiState.searchQuery.isEmpty()) {
            CuratedAudiophileCarRow(
                onStationSelect = onStationSelect,
                onToggleFavorite = onToggleFavorite
            )
            Spacer(modifier = Modifier.height(6.dp))
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
            isAntiGlare = isAntiGlare,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CuratedAudiophileCarRow(
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit
) {
    val curatedStations = remember { CuratedStationsService.defaultCuratedStations }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.car_curated_master_title),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.badge_flac_aac),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(curatedStations) { station ->
                Surface(
                    onClick = { onStationSelect(station) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.width(180.dp).height(54.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = station.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (station.id.startsWith("curated_rp")) stringResource(R.string.badge_flac_master) else stringResource(R.string.badge_somafm),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutomotiveFavoritesPanel(
    favoriteStations: List<RadioStation>,
    currentStationId: String?,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    columns: Int,
    isAntiGlare: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.saved_favorites),
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
                        text = stringResource(R.string.car_no_favorites_desc),
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
                isAntiGlare = isAntiGlare,
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
    isAntiGlare: Boolean,
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
                        else if (isAntiGlare) Color(0xFF0D1117)
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
                            contentDescription = stringResource(R.string.favorites),
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
    isAntiGlare: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isAntiGlare) Color(0xFF0D1117) else MaterialTheme.colorScheme.surface,
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
                        text = if (isLoading) stringResource(R.string.buffering_stream) else (streamTitle ?: station.genre),
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
        String.format(Locale.US, "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
