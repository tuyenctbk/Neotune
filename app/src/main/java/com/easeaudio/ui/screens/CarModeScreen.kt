package com.easeaudio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.easeaudio.R
import com.easeaudio.data.RadioStation
import com.easeaudio.data.PodcastEpisode
import com.easeaudio.ui.components.AudioVisualizerCanvas
import com.easeaudio.ui.components.VisualizerStyle
import com.easeaudio.ui.theme.FavoriteHeartColor
import com.easeaudio.viewmodel.HomeUiState

enum class CarTab {
    Player, Radio, Podcast, Favorites
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
    onExitCarMode: () -> Unit
) {
    var activeCarTab by remember { mutableStateOf(CarTab.Player) }
    var showCountryDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isWide = this.maxWidth > 600.dp
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // SIDEBAR NAVIGATION - Minimalist & Safe
                    if (isWide) {
                        CarSideNav(
                            selectedTab = activeCarTab,
                            onTabSelect = { 
                                activeCarTab = it
                                if (it == CarTab.Radio) onTabSelect(HomeTab.Radio)
                                if (it == CarTab.Podcast) onTabSelect(HomeTab.Podcast)
                            },
                            onExit = onExitCarMode
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (!isWide) {
                            CarTopNav(
                                selectedTab = activeCarTab,
                                onTabSelect = { 
                                    activeCarTab = it 
                                    if (it == CarTab.Radio) onTabSelect(HomeTab.Radio)
                                    if (it == CarTab.Podcast) onTabSelect(HomeTab.Podcast)
                                },
                                onExit = onExitCarMode
                            )
                        }

                        // Content Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = if (isWide) 16.dp else 12.dp)
                                .padding(top = if (isWide) 12.dp else 0.dp)
                                .padding(bottom = if (activeCarTab != CarTab.Player && uiState.currentStation != null) 72.dp else 0.dp)
                        ) {
                            AnimatedContent(
                                targetState = activeCarTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "car_tab_transition"
                            ) { targetTab ->
                                when (targetTab) {
                                    CarTab.Player -> {
                                        UnifiedHeroPlayer(
                                            uiState = uiState,
                                            onPlayPause = onPlayPause,
                                            onNextStation = onNextStation,
                                            onPreviousStation = onPreviousStation,
                                            onSelectStation = onSelectStation,
                                            onToggleFavorite = onToggleFavorite,
                                            onEpisodeSelect = onEpisodeSelect,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    CarTab.Radio, CarTab.Podcast -> {
                                        DiscoveryPanel(
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
                                            columns = if (isWide) 3 else 1
                                        )
                                    }
                                    CarTab.Favorites -> {
                                        FavoritesPanel(
                                            favoriteStations = uiState.favoriteStations,
                                            onStationSelect = {
                                                onSelectStation(it)
                                                activeCarTab = CarTab.Player
                                            },
                                            onToggleFavorite = onToggleFavorite,
                                            columns = if (isWide) 3 else 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // MINI PLAYER - Pinned to bottom when browsing
                if (activeCarTab != CarTab.Player && uiState.currentStation != null) {
                    CarMiniPlayer(
                        station = uiState.currentStation!!,
                        isPlaying = uiState.isPlaying,
                        isLoading = uiState.isLoading,
                        streamTitle = uiState.streamTitle,
                        waveAmplitudes = uiState.waveAmplitudes,
                        onTogglePlay = onPlayPause,
                        onClick = { activeCarTab = CarTab.Player },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    )
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
private fun CarSideNav(
    selectedTab: CarTab,
    onTabSelect: (CarTab) -> Unit,
    onExit: () -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        header = {
            IconButton(onClick = onExit, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Exit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.width(86.dp).fillMaxHeight()
    ) {
        val items = listOf(
            Triple(CarTab.Player, Icons.Filled.PlayCircle, "Player"),
            Triple(CarTab.Radio, Icons.Filled.Radio, "Radio"),
            Triple(CarTab.Podcast, Icons.Filled.Mic, "Podcasts"),
            Triple(CarTab.Favorites, Icons.Filled.Favorite, "Saved")
        )

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { (tab, icon, label) ->
                val isSelected = selectedTab == tab
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onTabSelect(tab) },
                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(26.dp)) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.background,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CarTopNav(
    selectedTab: CarTab,
    onTabSelect: (CarTab) -> Unit,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
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
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelect(tab) },
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Text(
                        text = tab.name,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        IconButton(onClick = onExit) {
            Icon(Icons.Filled.Close, contentDescription = "Exit")
        }
    }
}

@Composable
private fun FavoritesPanel(
    favoriteStations: List<RadioStation>,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    columns: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Your Favorites",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (favoriteStations.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No saved stations", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            QuickSelectGrid(
                stations = favoriteStations,
                currentStationId = null,
                onSelectStation = onStationSelect,
                onToggleFavorite = onToggleFavorite,
                columns = columns,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun DiscoveryPanel(
    uiState: HomeUiState,
    onGenreSelect: (String) -> Unit,
    onCountryPickerOpen: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onStationSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onLoadMore: () -> Unit,
    columns: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Minimalist Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f).height(46.dp)
            )

            if (uiState.selectedTab == HomeTab.Radio) {
                // Nation Picker
                Button(
                    onClick = onCountryPickerOpen,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = uiState.selectedCountry, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Genre Filter
        val genreList = if (uiState.selectedTab == HomeTab.Radio) uiState.availableGenres else uiState.availablePodcastTopics
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genreList) { genre ->
                val isSelected = genre.key == uiState.selectedGenre
                Surface(
                    onClick = { onGenreSelect(genre.key) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                ) {
                    Text(
                        text = stringResource(genre.labelResId),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid
        QuickSelectGrid(
            stations = uiState.stations,
            currentStationId = uiState.currentStation?.id,
            onSelectStation = onStationSelect,
            onToggleFavorite = onToggleFavorite,
            onLoadMore = onLoadMore,
            columns = columns,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun UnifiedHeroPlayer(
    uiState: HomeUiState,
    onPlayPause: () -> Unit,
    onNextStation: () -> Unit,
    onPreviousStation: () -> Unit,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onEpisodeSelect: (RadioStation, PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStation = uiState.currentStation
    val isPlaying = uiState.isPlaying
    val waveAmplitudes = uiState.waveAmplitudes

    BoxWithConstraints(modifier = modifier) {
        val availableHeight = this.maxHeight
        val isExtremelyShort = availableHeight < 320.dp
        
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(if (isExtremelyShort) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side Area: Info & Controls - Proportional
                Column(
                    modifier = Modifier.weight(1.3f).fillMaxHeight().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top: Artwork & Basic Meta
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // VERY Compact Artwork for vertical reliability
                        Box(
                            modifier = Modifier
                                .size(if (isExtremelyShort) 80.dp else 100.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (currentStation?.imageUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = currentStation.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Radio,
                                    contentDescription = null,
                                    modifier = Modifier.size(if (isExtremelyShort) 40.dp else 52.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            }
                            
                            if (currentStation != null) {
                                IconButton(
                                    onClick = { onToggleFavorite(currentStation) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(32.dp)
                                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (currentStation.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (currentStation.isFavorite) FavoriteHeartColor else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = currentStation?.name ?: stringResource(R.string.no_station_selected),
                            style = if (isExtremelyShort) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black) 
                                    else MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentStation?.genre ?: "NeoTune Radio",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        if (currentStation != null && isPlaying) {
                            Spacer(modifier = Modifier.height(4.dp))
                            AudioVisualizerCanvas(
                                waveAmplitudes = waveAmplitudes,
                                isPlaying = isPlaying,
                                modifier = Modifier.fillMaxWidth(0.8f).height(20.dp),
                                style = VisualizerStyle.ROUNDED_BARS,
                                primaryColor = MaterialTheme.colorScheme.primary,
                                secondaryColor = MaterialTheme.colorScheme.secondary,
                                accentColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    // Integrated Playback Controls - PINNED TO BOTTOM
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousStation,
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Filled.SkipPrevious, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }

                        FilledIconButton(
                            onClick = onPlayPause,
                            modifier = Modifier.size(if (isExtremelyShort) 64.dp else 72.dp),
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.background
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = onNextStation,
                            modifier = Modifier
                                .size(52.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(if (isExtremelyShort) 8.dp else 16.dp))

                // Right Side Area: Detailed Selection List
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp)
                ) {
                    val isPodcast = currentStation?.isPodcast == true
                    Text(
                        text = if (isPodcast) "Episodes" else "Recently Played",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    if (isPodcast) {
                        if (uiState.isLoadingEpisodes) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(uiState.currentEpisodesList) { episode ->
                                    val isCurrent = uiState.currentEpisode?.id == episode.id
                                    Surface(
                                        onClick = { onEpisodeSelect(currentStation!!, episode) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = episode.artworkUrl.ifBlank { currentStation!!.imageUrl },
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = episode.title,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold),
                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = episode.pubDate,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Radio: Show Recent or Curated
                        QuickSelectGrid(
                            stations = uiState.recentRadioStations,
                            currentStationId = currentStation?.id,
                            onSelectStation = { onSelectStation(it) },
                            onToggleFavorite = onToggleFavorite,
                            columns = 1,
                            modifier = Modifier.fillMaxSize()
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
    onTogglePlay: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(68.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = station.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
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
                            modifier = Modifier.width(28.dp).height(12.dp),
                            style = VisualizerStyle.ROUNDED_BARS,
                            primaryColor = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
            
            Surface(
                onClick = onTogglePlay,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.background, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickSelectGrid(
    stations: List<RadioStation>,
    currentStationId: String?,
    onSelectStation: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit = {},
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

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        items(stations, key = { it.id }) { station ->
            val isSelected = currentStationId == station.id
            var isCardFocused by remember { mutableStateOf(false) }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (columns == 1) 56.dp else 68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .onFocusChanged { isCardFocused = it.isFocused }
                    .border(
                        width = if (isCardFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                        color = if (isCardFocused) MaterialTheme.colorScheme.primary else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectStation(station) },
                color = if (isCardFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar / Artwork
                    AsyncImage(
                        model = station.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(if (columns == 1) 38.dp else 48.dp).clip(RoundedCornerShape(6.dp))
                    )
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = station.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected || isCardFocused) FontWeight.Black else FontWeight.Bold,
                                fontSize = if (columns == 1) 14.sp else 15.sp
                            ),
                            color = if (isSelected || isCardFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
                    
                    // Heart Icon (Favorite)
                    IconButton(
                        onClick = { onToggleFavorite(station) },
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
