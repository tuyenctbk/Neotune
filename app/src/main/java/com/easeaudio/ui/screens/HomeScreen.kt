package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ads.AdMobBanner
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.components.AttributionDialog
import com.easeaudio.firebase.AppRemoteConfig
import com.easeaudio.network.NetworkStatus
import com.easeaudio.ui.theme.*
import com.easeaudio.viewmodel.GenreDisplay

enum class HomeTab { Radio, Podcast }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    stations: List<RadioStation>,
    recentStations: List<RadioStation>,
    currentStation: RadioStation?,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    isDiscoveringOnline: Boolean = false,
    failedStationIds: Set<String> = emptySet(),
    searchQuery: String,
    selectedGenre: String,
    availableGenres: List<GenreDisplay>,
    availablePodcastTopics: List<GenreDisplay> = emptyList(),
    selectedCountry: String = "Global",
    availableCountries: List<com.easeaudio.viewmodel.CountryDisplay> = emptyList(),
    isLoadingCountries: Boolean = false,
    sleepTimerRemaining: Int?,
    networkStatus: NetworkStatus = NetworkStatus(),
    remoteConfig: AppRemoteConfig = AppRemoteConfig(),
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = true,
    isDiscoveryError: Boolean = false,
    streamTitle: String? = null,
    initialTab: HomeTab = HomeTab.Radio,
    onPlayPause: () -> Unit = {},
    onNextStation: () -> Unit = {},
    onPreviousStation: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit,
    onGenreSelect: (String) -> Unit,
    onCountrySelect: (String) -> Unit = {},
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onOpenAddStation: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenNetworkConfig: () -> Unit = {},
    onOpenOnboarding: () -> Unit = {},
    onOpenBlockedDialog: () -> Unit = {},
    onBlockStation: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryDiscovery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var showAttributionDialog by remember { mutableStateOf(false) }
    var showAppearanceScreen by remember { mutableStateOf(false) }
    var showCarMode by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var showTrackActionSheet by remember { mutableStateOf(false) }
    var showCountrySelectionDialog by remember { mutableStateOf(false) }
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) {
        activeTab = initialTab
    }
    val displayedStations = stations
    val isFabVisible by remember(searchQuery, displayedStations) {
        derivedStateOf {
            searchQuery.isNotBlank() && displayedStations.isEmpty() && !isDiscoveringOnline
        }
    }

    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
        }
    }

    val isTv = rememberIsTv()

    LaunchedEffect(shouldLoadMore, canLoadMore, isLoadingMore) {
        if (shouldLoadMore && canLoadMore && !isLoadingMore) {
            onLoadMore()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.imePadding()
            ) {
                var isFabFocused by remember { mutableStateOf(false) }
                val showFabFocus = isFabFocused
                FloatingActionButton(
                    onClick = onOpenAddStation,
                    containerColor = if (showFabFocus) Color.White else NeonCyan,
                    contentColor = DarkBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onFocusChanged { isFabFocused = it.isFocused }
                        .border(
                            width = if (showFabFocus) 3.dp else 0.dp,
                            color = if (showFabFocus) NeonCyan else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("fab_add_station")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Custom Station")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 800.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
            // AdMob Banner (Configured via Firebase Remote Config)
            if (remoteConfig.adsEnabled) {
                item {
                    AdMobBanner(
                        adUnitId = remoteConfig.bannerAdUnitId,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // App Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_favicon),
                                contentDescription = "NeoTune Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.6).sp,
                                        lineHeight = 28.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.app_description),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = TextSecondary
                                )
                            }
                        }

                        if (activeTab == HomeTab.Radio) {
                            var isCountryPillFocused by remember { mutableStateOf(false) }
                            val currentCountryObj = availableCountries.find { it.name.equals(selectedCountry, ignoreCase = true) }
                            val flagEmoji = currentCountryObj?.flag ?: if (selectedCountry.equals("Global", ignoreCase = true)) "🌐" else "🏳️"
                            
                            Surface(
                                onClick = { showCountrySelectionDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCountryPillFocused) DarkSurfaceVariant else DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isCountryPillFocused) 1.5.dp else 1.dp,
                                    color = if (isCountryPillFocused) NeonCyan else CardBorder
                                ),
                                modifier = Modifier
                                    .onFocusChanged { isCountryPillFocused = it.isFocused }
                                    .testTag("btn_top_country_picker")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$flagEmoji ${currentCountryObj?.name ?: selectedCountry}",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select Country",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = if (activeTab == HomeTab.Podcast) "Search podcasts…" else stringResource(R.string.search_placeholder),
                            color = TextMuted
                        )
                    },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = if (activeTab == HomeTab.Podcast) NeonPurple else NeonCyan,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("input_search_stations")
                )
            }



            // Category / Topic Pills — shown on both Radio & Podcast tabs
            val currentPills = if (activeTab == HomeTab.Podcast) availablePodcastTopics else availableGenres
            val pillAccent = if (activeTab == HomeTab.Podcast) NeonPurple else NeonCyan

            if (currentPills.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentPills) { genre ->
                            val isSelected = genre.key == selectedGenre
                            var isPillFocused by remember { mutableStateOf(false) }
                            val showPillFocus = isPillFocused
                            Box(
                                modifier = Modifier
                                    .onFocusChanged { isPillFocused = it.isFocused }
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) pillAccent
                                        else if (showPillFocus) DarkSurfaceVariant
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (showPillFocus) 2.dp else if (isSelected) 0.dp else 1.dp,
                                        color = if (showPillFocus) pillAccent else if (isSelected) Color.Transparent else CardBorder,
                                        shape = CircleShape
                                    )
                                    .clickable { onGenreSelect(genre.key) }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                                    .testTag("genre_chip_${genre.key}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(genre.labelResId),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected || showPillFocus) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) DarkBackground else if (showPillFocus) pillAccent else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            // Featured Station Hero Banner — Radio mode only
            if (displayedStations.isNotEmpty() && searchQuery.isEmpty() && selectedGenre == "All" && activeTab == HomeTab.Radio) {
                val featured = displayedStations.first()
                item {
                    var isHeroFocused by remember { mutableStateOf(false) }
                    val showHeroFocus = isHeroFocused
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .onFocusChanged { isHeroFocused = it.isFocused }
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onStationSelect(featured) }
                            .border(
                                width = if (showHeroFocus) 2.5.dp else 0.dp,
                                color = if (showHeroFocus) NeonCyan else Color.Transparent,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .testTag("hero_featured_card"),
                        color = DarkSurface
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                            AsyncImage(
                                model = featured.imageUrl,
                                contentDescription = featured.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, DarkBackground.copy(alpha = 0.95f))
                                        )
                                    )
                            )
                            // Content
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.featured_station),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = featured.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${featured.genre} • ${featured.bitrate}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val isFeaturedSelected = currentStation?.id == featured.id
                                    if (isFeaturedSelected && isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = DarkBackground,
                                            strokeWidth = 2.5.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isFeaturedSelected && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Play Featured",
                                            tint = DarkBackground
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section Title
            item {
                val sectionTitle = when {
                    searchQuery.isNotBlank() -> stringResource(R.string.search_results)
                    selectedGenre == "Favorites" -> stringResource(R.string.your_favorite_stations)
                    activeTab == HomeTab.Podcast -> stringResource(R.string.podcasts_and_shows)
                    else -> stringResource(R.string.live_radio_stations)
                }
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }

            // Empty or Initial Loading State
            if (displayedStations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDiscoveringOnline) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = if (activeTab == HomeTab.Podcast) NeonPurple else NeonCyan,
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (activeTab == HomeTab.Podcast) "Discovering podcasts & shows..." else stringResource(R.string.loading_more_stations),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                            }
                        } else if (isDiscoveryError) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.network_error_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onRetryDiscovery,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.retry_discovery), color = DarkBackground)
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (activeTab == HomeTab.Podcast) "No podcasts found matching your search." else stringResource(R.string.no_stations_found),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (activeTab != HomeTab.Podcast) {
                                    Text(stringResource(R.string.add_station_prompt), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Station Cards List
            items(displayedStations, key = { it.id }) { station ->
                val isSelected = currentStation?.id == station.id
                val isUnreachable = failedStationIds.contains(station.id)
                StationCard(
                    station = station,
                    isSelected = isSelected,
                    isPlaying = isSelected && isPlaying,
                    isLoading = isSelected && isLoading,
                    isUnreachable = isUnreachable,
                    onSelect = { onStationSelect(station) },
                    onToggleFavorite = { onToggleFavorite(station) },
                    onBlockStation = { onBlockStation(station.id) }
                )
            }

            if (isLoadingMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.loading_more_stations),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

    if (showAttributionDialog) {
        AttributionDialog(onDismiss = { showAttributionDialog = false })
    }

    if (showAppearanceScreen) {
        val context = androidx.compose.ui.platform.LocalContext.current
        AppearanceSelectionScreen(
            currentTheme = AppThemeState.currentTheme,
            themes = AppThemeState.ThemePresets,
            onDismiss = { showAppearanceScreen = false },
            onSelectTheme = { theme ->
                AppThemeState.saveTheme(context, theme.id)
            }
        )
    }

    if (showCarMode) {
        CarModeScreen(
            currentStation = currentStation,
            isPlaying = isPlaying,
            stations = stations,
            onPlayPause = onPlayPause,
            onNextStation = { onNextStation() },
            onPreviousStation = { onPreviousStation() },
            onSelectStation = { onStationSelect(it) },
            onExitCarMode = { showCarMode = false }
        )
    }

    if (showAlarmDialog) {
        com.easeaudio.ui.components.AlarmDialog(
            currentStation = currentStation,
            onDismiss = { showAlarmDialog = false }
        )
    }

    if (showCountrySelectionDialog) {
        com.easeaudio.ui.components.CountrySelectionDialog(
            selectedCountry = selectedCountry,
            countries = availableCountries,
            isLoading = isLoadingCountries,
            onSelectCountry = {
                onCountrySelect(it)
                showCountrySelectionDialog = false
            },
            onDismiss = { showCountrySelectionDialog = false }
        )
    }

    if (showTrackActionSheet && !streamTitle.isNullOrBlank()) {
        com.easeaudio.ui.components.TrackActionSheet(
            trackTitle = streamTitle!!,
            stationName = currentStation?.name ?: "Radio",
            stationGenre = currentStation?.genre ?: "",
            isFavorite = currentStation?.isFavorite ?: false,
            onToggleFavorite = { currentStation?.let { onToggleFavorite(it) } },
            onSetAsAlarmStation = { showAlarmDialog = true },
            onBlockStation = { currentStation?.let { onBlockStation(it.id) } },
            onDismiss = { showTrackActionSheet = false }
        )
    }
}

@Composable
fun StationCard(
    station: RadioStation,
    isSelected: Boolean,
    isPlaying: Boolean,
    isLoading: Boolean = false,
    isUnreachable: Boolean = false,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onBlockStation: (() -> Unit)? = null
) {
    val isTv = rememberIsTv()
    var isFocused by remember { mutableStateOf(false) }
    val showFocus = isFocused
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .border(
                width = if (showFocus) 2.5.dp else if (isSelected) 1.dp else 0.dp,
                color = if (showFocus) NeonCyan else if (isSelected) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("station_card_${station.id}"),
        color = if (showFocus) DarkSurfaceVariant else if (isSelected) DarkSurfaceVariant.copy(alpha = 0.8f) else DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Station Image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = station.imageUrl,
                    contentDescription = station.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = NeonCyan,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = NeonCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                val isPodcast = station.isPodcast
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) (if (isPodcast) NeonPurple else NeonCyan) else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                if (isSelected && isLoading) {
                    Text(
                        text = stringResource(R.string.buffering_stream),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isUnreachable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.stream_unreachable),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val subtitleText = if (isPodcast) {
                            station.genre
                        } else if (station.country.isNotBlank()) {
                            "${station.genre} • ${station.country}"
                        } else {
                            station.genre
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (!isPodcast && station.bitrate.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkBackground.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = station.bitrate.replace("kbps", "k").trim(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = NeonCyan,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .focusProperties { canFocus = false }
                        .testTag("favorite_button_${station.id}")
                ) {
                    Icon(
                        imageVector = if (station.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (station.isFavorite) FavoriteHeartColor else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun AppearanceSelectionScreen(
    currentTheme: ThemePreset,
    themes: List<ThemePreset>,
    onDismiss: () -> Unit,
    onSelectTheme: (ThemePreset) -> Unit
) {
    val cafeThemeIds = listOf("espresso_bar", "bistro_warm", "fine_dining_obsidian", "garden_cafe", "wine_bar", "minimalist_cafe", "trattoria", "youth_cafe")
    val cafeThemes = themes.filter { cafeThemeIds.contains(it.id) }
    val standardThemes = themes.filter { !it.id.startsWith("youth_") && !cafeThemeIds.contains(it.id) }
    val youthThemes = themes.filter { theme -> theme.id.startsWith("youth_") && !cafeThemeIds.contains(theme.id) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .clip(CircleShape)
                            .background(if (isBackFocused) NeonCyan else Color.Transparent)
                            .testTag("btn_close_appearance")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isBackFocused) DarkBackground else NeonCyan
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.appearance),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.appearance_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cafe Themes Section
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.theme_section_cafe),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = NeonPink,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_section_cafe_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    items(cafeThemes, key = { it.id }) { theme ->
                        ThemeSelectionCard(theme, currentTheme, onSelectTheme)
                    }

                    // Standard Section
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.theme_section_standard),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = NeonCyan,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_section_standard_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    items(standardThemes, key = { it.id }) { theme ->
                        ThemeSelectionCard(theme, currentTheme, onSelectTheme)
                    }

                    // Youth Section
                    item {
                        Column {
                            Text(
                                text = stringResource(R.string.theme_section_youth),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                color = NeonPurple,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                            Text(
                                text = stringResource(R.string.theme_section_youth_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                    items(youthThemes, key = { it.id }) { theme ->
                        ThemeSelectionCard(theme, currentTheme, onSelectTheme)
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionCard(
    theme: ThemePreset,
    currentTheme: ThemePreset,
    onSelectTheme: (ThemePreset) -> Unit
) {
    val isTv = rememberIsTv()
    val isSelected = theme.id == currentTheme.id
    var isFocused by remember { mutableStateOf(false) }
    val showFocus = isFocused

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelectTheme(theme) }
            .border(
                width = if (showFocus) 3.dp else if (isSelected) 1.5.dp else 1.dp,
                color = if (showFocus) theme.primary else if (isSelected) theme.primary.copy(alpha = 0.6f) else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("theme_card_${theme.id}"),
        color = if (showFocus) DarkSurfaceVariant else if (isSelected) DarkSurfaceVariant.copy(alpha = 0.5f) else DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Text Info (Left side)
            Column(modifier = Modifier.weight(1.3f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) theme.primary else TextPrimary
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.in_use),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = theme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Color dots info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorPill(label = stringResource(R.string.color_pill_bg), color = theme.background)
                    ColorPill(label = stringResource(R.string.color_pill_card), color = theme.surface)
                    ColorPill(label = stringResource(R.string.color_pill_accent), color = theme.primary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Mini Mockup (Right side - Illustration)
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.background)
                    .border(1.dp, theme.cardBorder, RoundedCornerShape(8.dp))
                    .padding(6.dp)
            ) {
                // Mini layout mimicking NeoTune App
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tiny title "NeoTune"
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(theme.textPrimary.copy(alpha = 0.8f))
                        )
                        // Tiny dot for status/menu
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(theme.primary)
                        )
                    }

                    // Mini active card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(theme.surface)
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Tiny image box
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(theme.textMuted.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Tiny text lines
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(theme.primary)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(2.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(theme.textSecondary.copy(alpha = 0.6f))
                                )
                            }
                            // Tiny play circle button
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(theme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(theme.background)
                                )
                            }
                        }
                    }

                    // Mini Bottom bar dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(theme.primary))
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(theme.textMuted.copy(alpha = 0.5f)))
                        Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(theme.textMuted.copy(alpha = 0.5f)))
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPill(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
                .border(0.5.dp, Color(0x33FFFFFF), CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

