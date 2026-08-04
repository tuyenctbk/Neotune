package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.easeaudio.R
import com.easeaudio.ads.AdMobBanner
import com.easeaudio.data.RadioStation
import com.easeaudio.ui.theme.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.easeaudio.viewmodel.HomeUiState

enum class HomeTab {
    Radio, Podcast
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    windowSizeClass: WindowSizeClass,
    onPlayPause: () -> Unit,
    onNextStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onGenreSelect: (String) -> Unit,
    onCountrySelect: (String) -> Unit,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onOpenAddStation: () -> Unit,
    onLoadMore: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryDiscovery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMedium = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
    val columns = when {
        isExpanded -> 4 // Optimize for TV and Large Tablets
        isMedium -> 2
        else -> 1
    }

    val gridState = rememberLazyGridState()
    var showCountryDialog by remember { mutableStateOf(false) }
    
    val isFabVisible by remember(uiState.searchQuery, uiState.stations) {
        derivedStateOf {
            uiState.searchQuery.isNotBlank() && uiState.stations.isEmpty() && !uiState.isDiscoveringOnline
        }
    }

    val shouldLoadMore by remember(gridState) {
        derivedStateOf {
            val totalItemsCount = gridState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemsCount > 0 && lastVisibleItemIndex >= totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore, uiState.canLoadMore, uiState.isLoadingMore) {
        if (shouldLoadMore && uiState.canLoadMore && !uiState.isLoadingMore) {
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
                FloatingActionButton(
                    onClick = onOpenAddStation,
                    containerColor = if (isFabFocused) Color.White else NeonCyan,
                    contentColor = DarkBackground,
                    shape = CircleShape,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .onFocusChanged { isFabFocused = it.isFocused }
                        .border(
                            width = if (isFabFocused) 3.dp else 0.dp,
                            color = if (isFabFocused) NeonCyan else Color.Transparent,
                            shape = CircleShape
                        )
                        .testTag("fab_add_station")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Custom Station")
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isDiscoveringOnline,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = if (isExpanded) 1200.dp else 800.dp)
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // AdMob Banner
                if (uiState.remoteConfig.adsEnabled) {
                    item(span = { GridItemSpan(columns) }) {
                        AdMobBanner(
                            adUnitId = uiState.remoteConfig.bannerAdUnitId,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // App Header
                item(span = { GridItemSpan(columns) }) {
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
                                    modifier = Modifier.size(if (isExpanded) 48.dp else 36.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = (if (isExpanded) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium).copy(
                                        fontSize = if (isExpanded) 36.sp else 28.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.8).sp
                                    ),
                                    color = TextPrimary
                                )
                            }

                            if (uiState.selectedTab == HomeTab.Radio) {
                                val currentCountryObj = uiState.availableCountries.find { it.name == uiState.selectedCountry }
                                val isGlobal = uiState.selectedCountry == "Global" || uiState.selectedCountry == "All" || currentCountryObj?.code?.isEmpty() == true
                                val flag = currentCountryObj?.flag ?: "🌐"

                                IconButton(
                                    onClick = { showCountryDialog = true },
                                    modifier = Modifier.testTag("btn_header_country_picker")
                                ) {
                                    if (isGlobal) {
                                        Icon(
                                            imageVector = Icons.Filled.Language,
                                            contentDescription = "Global",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(text = flag, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.app_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = if (isExpanded) 58.dp else 46.dp)
                        )
                    }
                }

                // Search Bar
                item(span = { GridItemSpan(columns) }) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text(stringResource(R.string.search_placeholder), color = TextMuted) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonCyan,
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

                // Genre Filter Pills
                item(span = { GridItemSpan(columns) }) {
                    val activeGenreList = if (uiState.selectedTab == HomeTab.Podcast) uiState.availablePodcastTopics else uiState.availableGenres
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeGenreList) { genre ->
                            val isSelected = genre.key == uiState.selectedGenre
                            var isPillFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .onFocusChanged { isPillFocused = it.isFocused }
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) NeonCyan 
                                        else if (isPillFocused) DarkSurfaceVariant 
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = if (isPillFocused) 2.dp else if (isSelected) 0.dp else 1.dp,
                                        color = if (isPillFocused) NeonCyan else if (isSelected) Color.Transparent else CardBorder,
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
                                        fontWeight = if (isSelected || isPillFocused) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) DarkBackground else if (isPillFocused) NeonCyan else TextMuted
                                )
                            }
                        }
                    }
                }

                // Recent Streams Section (Tab-filtered)
                val activeRecentList = if (uiState.selectedTab == HomeTab.Radio) uiState.recentRadioStations else uiState.recentPodcastStations
                if (activeRecentList.isNotEmpty() && uiState.searchQuery.isEmpty() && uiState.selectedGenre == "All") {
                    item(span = { GridItemSpan(columns) }) {
                        Text(
                            text = stringResource(R.string.recent_streams),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                    item(span = { GridItemSpan(columns) }) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(activeRecentList) { station ->
                                RecentStationCard(
                                    station = station,
                                    isPlaying = uiState.currentStation?.id == station.id && uiState.isPlaying,
                                    onClick = { onStationSelect(station) }
                                )
                            }
                        }
                    }
                }

                // Featured Station Hero Banner
                if (uiState.stations.isNotEmpty() && uiState.searchQuery.isEmpty() && uiState.selectedGenre == "All") {
                    val featured = uiState.stations.first()
                    item(span = { GridItemSpan(columns) }) {
                        var isHeroFocused by remember { mutableStateOf(false) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .onFocusChanged { isHeroFocused = it.isFocused }
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onStationSelect(featured) }
                                .border(
                                    width = if (isHeroFocused) 2.5.dp else 0.dp,
                                    color = if (isHeroFocused) NeonCyan else Color.Transparent,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .testTag("hero_featured_card"),
                            color = DarkSurface
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(if (isExpanded) 240.dp else 160.dp)) {
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
                                        .padding(if (isExpanded) 24.dp else 16.dp),
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
                                            style = if (isExpanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
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
                                            .size(if (isExpanded) 64.dp else 48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val isFeaturedSelected = uiState.currentStation?.id == featured.id
                                        if (isFeaturedSelected && uiState.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(if (isExpanded) 32.dp else 24.dp),
                                                color = DarkBackground,
                                                strokeWidth = 2.5.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isFeaturedSelected && uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = "Play Featured",
                                                tint = DarkBackground,
                                                modifier = Modifier.size(if (isExpanded) 32.dp else 24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section Title
                item(span = { GridItemSpan(columns) }) {
                    Text(
                        text = if (uiState.selectedTab == HomeTab.Radio) stringResource(R.string.live_radio_stations) else stringResource(R.string.podcasts_and_shows),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }

                // Empty or Initial Loading State
                if (uiState.stations.isEmpty()) {
                    item(span = { GridItemSpan(columns) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isDiscoveringOnline) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = NeonCyan,
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.loading_more_stations),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextSecondary
                                    )
                                }
                            } else if (uiState.isDiscoveryError) {
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
                                    Text(stringResource(R.string.no_stations_found), style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(stringResource(R.string.add_station_prompt), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }
                        }
                    }
                }

                // Station Cards List
                items(uiState.stations, key = { it.id }) { station ->
                    val isSelected = uiState.currentStation?.id == station.id
                    val isUnreachable = uiState.failedStationIds.contains(station.id)
                    StationCard(
                        station = station,
                        isSelected = isSelected,
                        isPlaying = isSelected && uiState.isPlaying,
                        isLoading = isSelected && uiState.isLoading,
                        isUnreachable = isUnreachable,
                        onSelect = { onStationSelect(station) },
                        onToggleFavorite = { onToggleFavorite(station) }
                    )
                }

                if (uiState.isLoadingMore) {
                    item(span = { GridItemSpan(columns) }) {
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

    if (showCountryDialog) {
        com.easeaudio.ui.components.CountrySelectionDialog(
            selectedCountry = uiState.selectedCountry,
            countries = uiState.availableCountries,
            onSelectCountry = { 
                onCountrySelect(it)
                showCountryDialog = false 
            },
            onDismiss = { showCountryDialog = false }
        )
    }
}

@Composable
fun RecentStationCard(
    station: RadioStation,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(100.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .clip(RoundedCornerShape(12.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) NeonCyan else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            AsyncImage(
                model = station.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = station.name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isFocused) NeonCyan else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
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
    onToggleFavorite: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .border(
                width = if (isFocused) 2.5.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) NeonCyan else if (isSelected) NeonCyan.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("station_card_${station.id}"),
        color = if (isFocused) DarkSurfaceVariant else if (isSelected) DarkSurfaceVariant.copy(alpha = 0.8f) else DarkSurface
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
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) NeonCyan else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (isSelected && isLoading) {
                    Text(
                        text = stringResource(R.string.buffering_stream),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isUnreachable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = station.genre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(" • ", color = TextMuted)
                        Text(
                            text = station.country,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(40.dp)
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
