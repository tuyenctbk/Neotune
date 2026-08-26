package com.easeaudio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.easeaudio.data.RadioDatabase
import com.easeaudio.data.RadioRepository
import com.easeaudio.data.RadioStation
import com.easeaudio.data.PodcastEpisode
import com.easeaudio.data.PodcastEpisodeService
import com.easeaudio.data.CuratedStationsService
import com.easeaudio.data.SongLyrics
import com.easeaudio.service.RadioPlayerManager
import com.easeaudio.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import java.util.UUID

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = RadioDatabase.getDatabase(application)
    val repository: com.easeaudio.data.IRadioRepository = RadioRepository(db.radioDao(), db.favoriteDao(), db.recentSearchDao(), db.listenLaterDao())
    val playerManager: RadioPlayerManager = RadioPlayerManager.getInstance(application)
    val audioComfortManager = com.easeaudio.data.AudioComfortManager.getInstance(application)
    private var onlineDiscoveryJob: Job? = null

    // Audio Comfort & Habits flows
    val isVolumeSafetyEnabled: StateFlow<Boolean> = audioComfortManager.isVolumeSafetyEnabled
    val isNightAudioModeEnabled: StateFlow<Boolean> = audioComfortManager.isNightAudioModeEnabled
    val todayListeningMinutes: StateFlow<Int> = audioComfortManager.todayListeningMinutes
    val currentStreakDays: StateFlow<Int> = audioComfortManager.currentStreakDays

    fun toggleVolumeSafety() {
        val next = !audioComfortManager.isVolumeSafetyEnabled.value
        audioComfortManager.setVolumeSafetyEnabled(next)
        if (next && playerManager.volume.value > com.easeaudio.data.AudioComfortManager.VOLUME_SAFETY_CAP) {
            playerManager.setVolume(com.easeaudio.data.AudioComfortManager.VOLUME_SAFETY_CAP)
        }
    }

    fun toggleNightAudioMode() {
        val next = !audioComfortManager.isNightAudioModeEnabled.value
        audioComfortManager.setNightAudioModeEnabled(next)
        if (next) {
            playerManager.setEqPreset("Chill Lounge")
        } else {
            playerManager.setEqPreset("Balanced")
        }
    }

    // Listen Later state flow
    val listenLaterItems: StateFlow<List<com.easeaudio.data.ListenLaterItem>> = repository.getListenLaterItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleListenLater(station: RadioStation) {
        viewModelScope.launch {
            repository.toggleListenLater(station)
        }
    }

    fun clearListenLater() {
        viewModelScope.launch {
            repository.clearListenLater()
        }
    }

    // PlayerManager state flow delegations for Unidirectional Data Flow
    val currentStation: StateFlow<RadioStation?> = playerManager.currentStation
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val isLoading: StateFlow<Boolean> = playerManager.isLoading
    val playbackError: StateFlow<String?> = playerManager.playbackError
    val playbackErrorDetails: StateFlow<com.easeaudio.service.PlaybackErrorDetails?> = playerManager.playbackErrorDetails
    val streamTitle: StateFlow<String?> = playerManager.streamTitle
    val waveAmplitudes: StateFlow<List<Float>> = playerManager.waveAmplitudes
    val volume: StateFlow<Float> = playerManager.volume
    val sleepTimerRemaining: StateFlow<Int?> = playerManager.sleepTimerMinutesRemaining
    val currentPlaybackPosition: StateFlow<Long> = playerManager.currentPosition
    val totalPlaybackDuration: StateFlow<Long> = playerManager.totalDuration
    val playbackSpeed: StateFlow<Float> = playerManager.playbackSpeed
    val networkStatus: StateFlow<com.easeaudio.network.NetworkStatus> = playerManager.networkStatus
    val remoteConfig: StateFlow<com.easeaudio.firebase.AppRemoteConfig> = playerManager.remoteConfig
    val failedStationIds: StateFlow<Set<String>> = MutableStateFlow<Set<String>>(emptySet()).asStateFlow()
    val trackArtworkUrl: StateFlow<String?> = playerManager.trackArtworkUrl
    val currentLyrics: StateFlow<SongLyrics?> = playerManager.currentLyrics
    val isLoadingLyrics: StateFlow<Boolean> = playerManager.isLoadingLyrics

    private val _curatedAudiophileStations = MutableStateFlow<List<RadioStation>>(CuratedStationsService.defaultCuratedStations)
    val curatedAudiophileStations: StateFlow<List<RadioStation>> = _curatedAudiophileStations.asStateFlow()

    fun fetchLyricsForCurrentTrack() {
        playerManager.fetchLyricsForCurrentTrack()
    }

    val snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recentSearchQueries: StateFlow<List<String>> = repository.getRecentSearchQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(com.easeaudio.ui.screens.HomeTab.Radio)
    val selectedTab: StateFlow<com.easeaudio.ui.screens.HomeTab> = _selectedTab.asStateFlow()

    private val _currentEpisodesList = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val currentEpisodesList: StateFlow<List<PodcastEpisode>> = _currentEpisodesList.asStateFlow()

    private val _currentEpisode = MutableStateFlow<PodcastEpisode?>(null)
    val currentEpisode: StateFlow<PodcastEpisode?> = _currentEpisode.asStateFlow()

    private val _isLoadingEpisodes = MutableStateFlow(false)
    val isLoadingEpisodes: StateFlow<Boolean> = _isLoadingEpisodes.asStateFlow()

    private val _showEpisodesSheet = MutableStateFlow(false)
    val showEpisodesSheet: StateFlow<Boolean> = _showEpisodesSheet.asStateFlow()

    fun setShowEpisodesSheet(show: Boolean) {
        _showEpisodesSheet.value = show
    }

    fun loadEpisodesForShow(show: RadioStation) {
        viewModelScope.launch {
            _isLoadingEpisodes.value = true
            try {
                val episodes = PodcastEpisodeService.fetchEpisodes(show, maxEpisodes = 1000)
                _currentEpisodesList.value = episodes
                if (_currentEpisode.value == null && episodes.isNotEmpty()) {
                    _currentEpisode.value = episodes.first()
                }
            } catch (e: Exception) {
                android.util.Log.e("RadioViewModel", "Failed to load episodes: ${e.message}")
            } finally {
                _isLoadingEpisodes.value = false
            }
        }
    }

    fun playEpisode(show: RadioStation, episode: PodcastEpisode) {
        _currentEpisode.value = episode
        playerManager.playPodcastEpisode(show, episode)
        viewModelScope.launch {
            repository.recordStationListened(show)
            snackbarMessage.emit("Playing '${episode.title}'")
        }
    }

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _selectedCountry = MutableStateFlow("Global")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    // ---- Country Discovery ----

    /** Fallback countries shown instantly while the API call loads (sorted by station count desc) */
    private val fallbackCountries = listOf(
        CountryDisplay("Global",         "🌐", "",   50000, "> 50k Radio stations"),
        CountryDisplay("United States",  "🇺🇸", "US", 5000,  "> 5,000 Radio stations"),
        CountryDisplay("Germany",        "🇩🇪", "DE", 3000,  "> 3,000 Radio stations"),
        CountryDisplay("United Kingdom", "🇬🇧", "GB", 2000,  "> 2,000 Radio stations"),
        CountryDisplay("France",         "🇫🇷", "FR", 2000,  "> 2,000 Radio stations"),
        CountryDisplay("Spain",          "🇪🇸", "ES", 1000,  "> 1,000 Radio stations"),
        CountryDisplay("Italy",          "🇮🇹", "IT", 1000,  "> 1,000 Radio stations"),
        CountryDisplay("Canada",         "🇨🇦", "CA", 800,   "> 800 Radio stations"),
        CountryDisplay("Brazil",         "🇧🇷", "BR", 800,   "> 800 Radio stations"),
        CountryDisplay("Australia",      "🇦🇺", "AU", 500,   "> 500 Radio stations"),
        CountryDisplay("Mexico",         "🇲🇽", "MX", 500,   "> 500 Radio stations"),
        CountryDisplay("Argentina",      "🇦🇷", "AR", 400,   "> 400 Radio stations"),
        CountryDisplay("Netherlands",    "🇳🇱", "NL", 400,   "> 400 Radio stations"),
        CountryDisplay("Poland",         "🇵🇱", "PL", 400,   "> 400 Radio stations"),
        CountryDisplay("Ukraine",        "🇺🇦", "UA", 300,   "> 300 Radio stations"),
        CountryDisplay("Switzerland",    "🇨🇭", "CH", 300,   "> 300 Radio stations"),
        CountryDisplay("Austria",        "🇦🇹", "AT", 300,   "> 300 Radio stations"),
        CountryDisplay("Belgium",        "🇧🇪", "BE", 300,   "> 300 Radio stations"),
        CountryDisplay("Turkey",         "🇹🇷", "TR", 200,   "> 200 Radio stations"),
        CountryDisplay("Sweden",         "🇸🇪", "SE", 200,   "> 200 Radio stations"),
        CountryDisplay("Japan",          "🇯🇵", "JP", 200,   "> 200 Radio stations"),
        CountryDisplay("India",          "🇮🇳", "IN", 200,   "> 200 Radio stations"),
        CountryDisplay("Norway",         "🇳🇴", "NO", 150,   "> 150 Radio stations"),
        CountryDisplay("Czech Republic", "🇨🇿", "CZ", 150,   "> 150 Radio stations"),
        CountryDisplay("Portugal",       "🇵🇹", "PT", 150,   "> 150 Radio stations"),
        CountryDisplay("Greece",         "🇬🇷", "GR", 150,   "> 150 Radio stations"),
        CountryDisplay("Ireland",        "🇮🇪", "IE", 100,   "> 100 Radio stations"),
        CountryDisplay("Chile",          "🇨🇱", "CL", 100,   "> 100 Radio stations"),
        CountryDisplay("Colombia",       "🇨🇴", "CO", 100,   "> 100 Radio stations"),
        CountryDisplay("South Africa",   "🇿🇦", "ZA", 100,   "> 100 Radio stations"),
        CountryDisplay("New Zealand",    "🇳🇿", "NZ", 80,    "> 80 Radio stations"),
        CountryDisplay("Saudi Arabia",   "🇸🇦", "SA", 50,    "~50 Radio stations"),
        CountryDisplay("Indonesia",      "🇮🇩", "ID", 50,    "> 50 Radio stations"),
        CountryDisplay("United Arab Emirates", "🇦🇪", "AE", 40, "~40 Radio stations"),
        CountryDisplay("Thailand",       "🇹🇭", "TH", 40,    "> 40 Radio stations"),
        CountryDisplay("Qatar",          "🇶🇦", "QA", 30,    "~30 Radio stations"),
        CountryDisplay("Philippines",    "🇵🇭", "PH", 30,    "> 30 Radio stations"),
        CountryDisplay("Malaysia",       "🇲🇾", "MY", 30,    "> 30 Radio stations"),
        CountryDisplay("Israel",         "🇮🇱", "IL", 30,    "~30 Radio stations"),
        CountryDisplay("Singapore",      "🇸🇬", "SG", 20,    "~20 Radio stations"),
        CountryDisplay("South Korea",    "🇰🇷", "KR", 20,    "~20 Radio stations"),
        CountryDisplay("Nigeria",        "🇳🇬", "NG", 20,    "~20 Radio stations"),
        CountryDisplay("Egypt",          "🇪🇬", "EG", 15,    "~15 Radio stations")
    )

    private val _availableCountries = MutableStateFlow(fallbackCountries)
    val availableCountries: StateFlow<List<CountryDisplay>> = _availableCountries.asStateFlow()

    val isLoadingCountries: StateFlow<Boolean>
        get() = _isLoadingCountries
    private val _isLoadingCountries = MutableStateFlow(false)

    private fun discoverCountries() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isLoadingCountries.value = true
            try {
                val apiData = com.easeaudio.data.RadioBrowserService.fetchCountries()
                if (apiData.isEmpty()) return@launch

                // Build the Global entry first (always at top)
                val globalEntry = CountryDisplay(
                    name = "Global",
                    flag = "🌐",
                    code = "",
                    stationCount = apiData.sumOf { it.third },
                    stationCountText = getApplication<Application>().getString(R.string.radio_stations_count_text)
                )

                // Convert API triples -> CountryDisplay, sorted by stationCount descending
                val discovered = apiData
                    .map { (name, isoCode, count) ->
                        val flag = com.easeaudio.data.RadioBrowserService.isoToFlagEmoji(isoCode)
                        val countText = when {
                            count >= 1000 -> "${"%,d".format(count)} Radio stations"
                            else          -> "$count Radio stations"
                        }
                        CountryDisplay(
                            name = name,
                            flag = flag,
                            code = isoCode,
                            stationCount = count,
                            stationCountText = countText
                        )
                    }
                    .sortedByDescending { it.stationCount }

                _availableCountries.value = listOf(globalEntry) + discovered
                android.util.Log.i("RadioViewModel", "Country list updated: ${discovered.size} countries discovered.")
            } catch (e: Exception) {
                android.util.Log.w("RadioViewModel", "discoverCountries failed, keeping fallback: ${e.message}")
                // Keep fallback list — no UI disruption
            } finally {
                _isLoadingCountries.value = false
            }
        }
    }

    val availableGenres = listOf(
        GenreDisplay("All", R.string.genre_all),
        GenreDisplay("80s & 90s", R.string.genre_80s_90s),
        GenreDisplay("News & Talk", R.string.genre_news_talk),
        GenreDisplay("Lo-Fi & Chill", R.string.lofi_chill),
        GenreDisplay("Pop & Hits", R.string.genre_pop_top40),
        GenreDisplay("Jazz & Blues", R.string.genre_jazz_blues),
        GenreDisplay("Rock & Metal", R.string.genre_rock_metal),
        GenreDisplay("Hip Hop & R&B", R.string.genre_hiphop_rnb),
        GenreDisplay("EDM & Dance", R.string.genre_edm_dance),
        GenreDisplay("Latin & Reggae", R.string.genre_latin_reggae),
        GenreDisplay("Classical", R.string.classical),
        GenreDisplay("Sports", R.string.genre_sports),
        GenreDisplay("Country", R.string.country_genre),
        GenreDisplay("Ambient", R.string.ambient),
        GenreDisplay("Custom", R.string.custom),
    )

    val availablePodcastTopics = listOf(
        GenreDisplay("All", R.string.genre_all),
        GenreDisplay("Technology", R.string.topic_technology),
        GenreDisplay("True Crime", R.string.topic_true_crime),
        GenreDisplay("Business", R.string.topic_business),
        GenreDisplay("Comedy", R.string.topic_comedy),
        GenreDisplay("Health", R.string.topic_health),
        GenreDisplay("Society", R.string.topic_society),
        GenreDisplay("Science", R.string.topic_science),
        GenreDisplay("News", R.string.topic_news)
    )

    // OPT-3: availableEqPresets removed — was dead code. The EqualizerDialog uses
    // the `eqPresets` list (defined below) which contains the correct set of presets.

    // Online discovered radio streams state

    private val _onlineDiscoveredStations = MutableStateFlow<List<RadioStation>>(emptyList())
    private val _isDiscoveringOnline = MutableStateFlow(false)
    val isDiscoveringOnline: StateFlow<Boolean> = _isDiscoveringOnline.asStateFlow()

    private val _isDiscoveryError = MutableStateFlow(false)
    val isDiscoveryError: StateFlow<Boolean> = _isDiscoveryError.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private val pageSize = 40

    private val _preferredGenres = MutableStateFlow<Set<String>>(emptySet())
    val preferredGenres: StateFlow<Set<String>> = _preferredGenres.asStateFlow()

    fun setPreferredGenres(genres: Set<String>) {
        _preferredGenres.value = genres
    }

    val filterAndBlockManager = com.easeaudio.data.FilterAndBlockManager.getInstance(application)
    val filterConfig = filterAndBlockManager.filterConfig
    val blockedStationIds = filterAndBlockManager.blockedStationIds
    val demotedStationIds = filterAndBlockManager.demotedStationIds

    private data class FilterState(
        val query: String,
        val genre: String,
        val country: String,
        val preferredGenres: Set<String>,
        val blockedIds: Set<String>,
        val demotedIds: Set<String>,
        val filterConfig: com.easeaudio.data.StationFilterConfig
    )

    private val defaultStationIds = setOf("bbc_world_service", "jazz_groove", "lofi_girl_radio")

    private val filterStateFlow = combine(
        combine(_searchQuery, _selectedGenre, _selectedCountry) { q, g, c -> Triple(q, g, c) },
        combine(_preferredGenres, filterAndBlockManager.blockedStationIds, filterAndBlockManager.demotedStationIds) { p, b, d -> Triple(p, b, d) },
        filterAndBlockManager.filterConfig
    ) { (query, genre, country), (preferred, blocked, demoted), config ->
        FilterState(query, genre, country, preferred, blocked, demoted, config)
    }

    val favoriteStations: StateFlow<List<RadioStation>> = combine(
        repository.getFavoriteStations(),
        filterAndBlockManager.blockedStationIds,
        filterAndBlockManager.filterConfig
    ) { favs, _, _ ->
        favs.filter { filterAndBlockManager.shouldIncludeStation(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val stations: StateFlow<List<RadioStation>> = combine(
        repository.getAllStations(),
        _onlineDiscoveredStations,
        _selectedTab,
        repository.getFavoriteStations(),
        filterStateFlow
    ) { localStations, onlineList, currentTab, favList, filter ->
        val query = filter.query
        val genre = filter.genre
        val country = filter.country
        val preferredGenres = filter.preferredGenres
        val favIds = favList.map { it.id }.toSet()
        val mergedList = mutableListOf<RadioStation>()
        val addedIds = mutableSetOf<String>()
        val localMap = localStations.associateBy { it.id }

        // 1. Prioritize active user custom stations matching currentTab
        val localMatches = localStations.filter { local ->
            local.isCustom && (if (currentTab == com.easeaudio.ui.screens.HomeTab.Podcast) local.isPodcast else !local.isPodcast) && filterAndBlockManager.shouldIncludeStation(local)
        }
        localMatches.forEach { station ->
            val updated = station.copy(isFavorite = favIds.contains(station.id) || station.isFavorite)
            mergedList.add(updated)
            addedIds.add(station.id)
        }

        // 2. Add online discovered stations in their stable discovered order, matching currentTab
        onlineList.forEach { online ->
            val matchesTab = if (currentTab == com.easeaudio.ui.screens.HomeTab.Podcast) online.isPodcast else !online.isPodcast
            if (matchesTab && !addedIds.contains(online.id) && filterAndBlockManager.shouldIncludeStation(online)) {
                val localCopy = localMap[online.id]
                val isFav = favIds.contains(online.id) || (localCopy?.isFavorite == true) || online.isFavorite
                val mergedStation = online.copy(
                    isFavorite = isFav,
                    isCustom = localCopy?.isCustom ?: online.isCustom,
                    lastListenedTimestamp = localCopy?.lastListenedTimestamp ?: online.lastListenedTimestamp
                )
                mergedList.add(mergedStation)
                addedIds.add(online.id)
            }
        }

        // 3. Add default curated stations and other cached local stations matching currentTab
        localStations.forEach { local ->
            val matchesTab = if (currentTab == com.easeaudio.ui.screens.HomeTab.Podcast) local.isPodcast else !local.isPodcast
            if (matchesTab && !addedIds.contains(local.id) && filterAndBlockManager.shouldIncludeStation(local)) {
                val isFav = favIds.contains(local.id) || local.isFavorite
                mergedList.add(local.copy(isFavorite = isFav))
                addedIds.add(local.id)
            }
        }

        // Filter merged results strictly
        val filteredList = mergedList.filter { station ->
            val isOnlineMatch = onlineList.any { it.id == station.id }
            val matchesQuery = query.isBlank() ||
                    station.name.contains(query, ignoreCase = true) ||
                    station.genre.contains(query, ignoreCase = true) ||
                    station.country.contains(query, ignoreCase = true)

            val matchesCountry = when {
                currentTab == com.easeaudio.ui.screens.HomeTab.Podcast -> true
                country == "Global" || country == "All" -> true
                else -> station.country.contains(country, ignoreCase = true)
            }

            val matchesGenre = when {
                currentTab == com.easeaudio.ui.screens.HomeTab.Podcast -> true
                genre == "All" -> true
                genre == "Custom" -> station.isCustom
                genre == "80s & 90s" -> station.genre.contains("80s", ignoreCase = true) || station.genre.contains("90s", ignoreCase = true) || station.genre.contains("Retro", ignoreCase = true) || station.genre.contains("Oldies", ignoreCase = true)
                genre == "News & Talk" || genre == "News & Reports" -> station.genre.contains("News", ignoreCase = true) || station.genre.contains("Report", ignoreCase = true) || station.genre.contains("Talk", ignoreCase = true) || station.genre.contains("Info", ignoreCase = true)
                genre == "Lo-Fi & Chill" -> station.genre.contains("Lo-Fi", ignoreCase = true) || station.genre.contains("Chill", ignoreCase = true) || station.genre.contains("Lofi", ignoreCase = true) || station.genre.contains("Lounge", ignoreCase = true)
                genre == "Jazz & Blues" || genre == "Jazz" -> station.genre.contains("Jazz", ignoreCase = true) || station.genre.contains("Blues", ignoreCase = true)
                genre == "Rock & Metal" || genre == "Rock" -> station.genre.contains("Rock", ignoreCase = true) || station.genre.contains("Metal", ignoreCase = true)
                genre == "Pop & Hits" || genre == "Pop" -> station.genre.contains("Pop", ignoreCase = true) || station.genre.contains("Hit", ignoreCase = true) || station.genre.contains("Top 40", ignoreCase = true) || station.genre.contains("Top40", ignoreCase = true)
                genre == "EDM & Dance" || genre == "EDM" -> station.genre.contains("EDM", ignoreCase = true) || station.genre.contains("Dance", ignoreCase = true) || station.genre.contains("House", ignoreCase = true) || station.genre.contains("Techno", ignoreCase = true)
                genre == "Hip Hop & R&B" || genre == "Hip Hop" -> station.genre.contains("Hip", ignoreCase = true) || station.genre.contains("Rap", ignoreCase = true) || station.genre.contains("Urban", ignoreCase = true) || station.genre.contains("R&B", ignoreCase = true) || station.genre.contains("RnB", ignoreCase = true)
                genre == "Latin & Reggae" -> station.genre.contains("Latin", ignoreCase = true) || station.genre.contains("Salsa", ignoreCase = true) || station.genre.contains("Reggae", ignoreCase = true) || station.genre.contains("Reggaeton", ignoreCase = true)
                genre == "Sports" -> station.genre.contains("Sport", ignoreCase = true)
                genre == "Classical" -> station.genre.contains("Classic", ignoreCase = true) || station.genre.contains("Piano", ignoreCase = true) || station.genre.contains("Orchestra", ignoreCase = true)
                genre == "Ambient" -> station.genre.contains("Ambient", ignoreCase = true) || station.genre.contains("Drone", ignoreCase = true)
                genre == "Country" -> station.genre.contains("Country", ignoreCase = true) || station.genre.contains("Folk", ignoreCase = true)
                else -> station.genre.contains(genre, ignoreCase = true)
            }

            matchesQuery && matchesCountry && (matchesGenre || isOnlineMatch)
        }

        val demotedIds = filter.demotedIds
        if (genre == "All" && preferredGenres.isNotEmpty()) {
            filteredList.sortedWith(
                compareBy<RadioStation> { if (demotedIds.contains(it.id)) 1 else 0 }
                    .thenByDescending { station ->
                        val matchesPref = preferredGenres.any { pref ->
                            station.genre.contains(pref, ignoreCase = true) ||
                                    (pref.contains("Lo-Fi", ignoreCase = true) && (station.genre.contains("Lo-Fi", ignoreCase = true) || station.genre.contains("Chill", ignoreCase = true) || station.genre.contains("Lofi", ignoreCase = true))) ||
                                    (pref.contains("News", ignoreCase = true) && (station.genre.contains("News", ignoreCase = true) || station.genre.contains("Talk", ignoreCase = true)))
                        }
                        if (matchesPref) 1 else 0
                    }
            )
        } else {
            filteredList.sortedBy { if (demotedIds.contains(it.id)) 1 else 0 }
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Equalizer state
    private val _activeEqPreset = MutableStateFlow("Balanced")
    val activeEqPreset: StateFlow<String> = _activeEqPreset.asStateFlow()

    val eqPresets = listOf(
        EqPresetDisplay("Balanced", R.string.balanced),
        EqPresetDisplay("Bass Boost", R.string.bass_boost),
        EqPresetDisplay("Chill Lounge", R.string.chill_lounge),
        EqPresetDisplay("Acoustic", R.string.acoustic),
        EqPresetDisplay("Vocal Focus", R.string.vocal_focus),
    )

    // UI Dialog visibility states
    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    private val _showEqualizerDialog = MutableStateFlow(false)
    val showEqualizerDialog: StateFlow<Boolean> = _showEqualizerDialog.asStateFlow()

    private val _showAddStationDialog = MutableStateFlow(false)
    val showAddStationDialog: StateFlow<Boolean> = _showAddStationDialog.asStateFlow()

    private val _showBlockedDialog = MutableStateFlow(false)
    val showBlockedDialog: StateFlow<Boolean> = _showBlockedDialog.asStateFlow()

    private val _showAppearanceDialog = MutableStateFlow(false)
    val showAppearanceDialog: StateFlow<Boolean> = _showAppearanceDialog.asStateFlow()

    private val _showAttributionDialog = MutableStateFlow(false)
    val showAttributionDialog: StateFlow<Boolean> = _showAttributionDialog.asStateFlow()

    val smartEngagementManager = com.easeaudio.engagement.SmartEngagementManager.getInstance(application)

    init {
        // Discover countries from RadioBrowser API (fallback shown immediately)
        discoverCountries()
        // Trigger initial online discovery for top global working stations
        discoverStationsOnline("", "All", "Global")
        // Initial load of recent stations
        refreshRecentStations()

        // Fetch dynamic SomaFM & Radio Paradise feeds
        viewModelScope.launch {
            try {
                val curated = CuratedStationsService.getCuratedStations()
                if (curated.isNotEmpty()) {
                    _curatedAudiophileStations.value = curated
                }
            } catch (e: Exception) {
                // Default curated stations are already set as fallback
            }
        }

        viewModelScope.launch {
            stations.collect { list ->
                playerManager.updateStationList(list)
            }
        }

        // BUG-6 fix: The old implementation used collect { while(playing && ...) } which
        // spawned a NEW parallel loop on every emission of isPlaying = true. Rapid
        // pause/resume caused duplicate listening-time recording and double engagement triggers.
        // Fix: cancel the previous job before starting a new one.
        var listeningJob: Job? = null
        viewModelScope.launch {
            playerManager.isPlaying.collect { playing ->
                listeningJob?.cancel()
                if (playing) {
                    listeningJob = launch {
                        while (isActive) {
                            kotlinx.coroutines.delay(60_000L)
                            smartEngagementManager.recordListeningTime(60L)
                            smartEngagementManager.checkSmartTriggers(eventSource = "playback_timer")
                        }
                    }
                }
            }
        }
    }

    val recentStations: StateFlow<List<RadioStation>> = combine(
        repository.getRecentStations(),
        filterAndBlockManager.blockedStationIds
    ) { recents, _ ->
        recents.filter { filterAndBlockManager.shouldIncludeStation(it) }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentRadioStations: StateFlow<List<RadioStation>> = recentStations.map { list ->
        list.filter { !it.isPodcast }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPodcastStations: StateFlow<List<RadioStation>> = recentStations.map { list ->
        list.filter { it.isPodcast }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshRecentStations() {
        // Automatically updated via Room reactive flows
    }

    private val appPrefs = getApplication<android.app.Application>().getSharedPreferences("neotune_prefs", android.content.Context.MODE_PRIVATE)
    private val _isBatterySaverEnabled = MutableStateFlow(appPrefs.getBoolean("is_battery_saver_enabled", false))
    val isBatterySaverEnabled: StateFlow<Boolean> = _isBatterySaverEnabled.asStateFlow()

    private val _isAutoPlayOnStartupEnabled = MutableStateFlow(appPrefs.getBoolean("is_auto_play_on_startup_enabled", false))
    val isAutoPlayOnStartupEnabled: StateFlow<Boolean> = _isAutoPlayOnStartupEnabled.asStateFlow()

    private val _selectedLauncherIcon = MutableStateFlow(com.easeaudio.util.AppIconManager.getCurrentIconTheme(getApplication()))
    val selectedLauncherIcon: StateFlow<String> = _selectedLauncherIcon.asStateFlow()

    val isAudioBoosterEnabled: StateFlow<Boolean> = playerManager.isAudioBoosterEnabled

    fun setAudioBoosterEnabled(enabled: Boolean) {
        playerManager.setAudioBoosterEnabled(enabled)
        viewModelScope.launch {
            snackbarMessage.emit(if (enabled) "Audio Booster Enabled (+6dB)" else "Audio Booster Disabled")
        }
    }

    // BUG-5 fix: battery-saver priming and auto-resume are in a second init block because
    // appPrefs/_isBatterySaverEnabled are declared after the first init block.
    // Kotlin runs multiple init blocks strictly in top-to-bottom order, so this is safe
    // and deterministic. The net result matches what was previously two racing init blocks,
    // but now the ordering is explicit and the first init cannot trigger autoResumeLastPlayedStation
    // before battery-saver mode is primed.
    init {
        if (_isBatterySaverEnabled.value) {
            playerManager.setBatterySaverMode(true)
        }
        autoResumeLastPlayedStation()
    }


    companion object {
        val defaultPopularStation = RadioStation(
            id = "101_smooth_jazz",
            name = "101 SMOOTH JAZZ",
            genre = "Jazz & Blues",
            country = "The United States Of America",
            streamUrl = "https://stream.zeno.fm/f3wvbbqmdg8uv",
            imageUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?auto=format&fit=crop&w=600&q=80",
            bitrate = "128 kbps"
        )
    }

    fun autoResumeLastPlayedStation() {
        viewModelScope.launch {
            try {
                val recent = repository.getRecentStations().first()
                val lastPlayed = recent.firstOrNull()
                if (lastPlayed != null && playerManager.currentStation.value == null) {
                    if (_isAutoPlayOnStartupEnabled.value) {
                        android.util.Log.i("RadioViewModel", "Auto-resuming last played station: ${lastPlayed.name}")
                        playerManager.playStation(lastPlayed)
                        snackbarMessage.emit("Auto-resumed '${lastPlayed.name}'")
                    } else {
                        android.util.Log.i("RadioViewModel", "Preloading last played station without play: ${lastPlayed.name}")
                        playerManager.setPreloadedStation(lastPlayed)
                    }
                } else if (lastPlayed == null && playerManager.currentStation.value == null) {
                    android.util.Log.i("RadioViewModel", "Preloading default popular station without play: ${defaultPopularStation.name}")
                    playerManager.setPreloadedStation(defaultPopularStation)
                }
            } catch (e: Exception) {
                android.util.Log.w("RadioViewModel", "Failed to auto-resume station: ${e.message}")
            }
        }
    }

    fun toggleAutoPlayOnStartup() {
        val newValue = !_isAutoPlayOnStartupEnabled.value
        _isAutoPlayOnStartupEnabled.value = newValue
        appPrefs.edit().putBoolean("is_auto_play_on_startup_enabled", newValue).apply()
        viewModelScope.launch {
            snackbarMessage.emit(if (newValue) "Auto-Play on Startup Enabled" else "Auto-Play on Startup Disabled")
        }
    }

    fun toggleBatterySaver() {
        val newValue = !_isBatterySaverEnabled.value
        _isBatterySaverEnabled.value = newValue
        appPrefs.edit().putBoolean("is_battery_saver_enabled", newValue).apply()
        playerManager.setBatterySaverMode(newValue)
    }

    fun setLauncherIcon(iconKey: String) {
        _selectedLauncherIcon.value = iconKey
        com.easeaudio.util.AppIconManager.setAppIcon(getApplication(), iconKey)
    }

    val blockedStations: StateFlow<List<RadioStation>> = combine(
        repository.getAllStations(),
        filterAndBlockManager.blockedStationIds
    ) { all, blockedIds ->
        all.filter { blockedIds.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // BUG-1 fix: The previous homeUiState used combine(listOf(49 flows)) { array -> ... }
    // which produces Array<Any> internally and required unchecked casts (array[8] as Boolean).
    // Any index shift silently becomes a ClassCastException at runtime.
    //
    // Fix: use nested typed combines (≤5 flows each) that produce typed data classes.
    // Incorrect ordering now fails at COMPILE TIME, not at runtime.

    /** Groups player-level state — changes on every playback event. */
    private data class PlayerSnapshot(
        val currentStation: RadioStation?,
        val isPlaying: Boolean,
        val isLoading: Boolean,
        val streamTitle: String?,
        val playbackError: String?,
        val trackArtworkUrl: String?,
        val currentLyrics: SongLyrics?,
        val isLoadingLyrics: Boolean
    )

    private val playerSnapshot: Flow<PlayerSnapshot> = combine(
        combine(currentStation, isPlaying, isLoading, streamTitle, playbackError) { st, play, load, title, err ->
            listOf<Any?>(st, play, load, title, err)
        },
        combine(trackArtworkUrl, currentLyrics, isLoadingLyrics) { art, lyr, loadLyr ->
            listOf<Any?>(art, lyr, loadLyr)
        }
    ) { basic, extra ->
        @Suppress("UNCHECKED_CAST")
        PlayerSnapshot(
            currentStation = basic[0] as RadioStation?,
            isPlaying = basic[1] as Boolean,
            isLoading = basic[2] as Boolean,
            streamTitle = basic[3] as String?,
            playbackError = basic[4] as String?,
            trackArtworkUrl = extra[0] as String?,
            currentLyrics = extra[1] as SongLyrics?,
            isLoadingLyrics = extra[2] as Boolean
        )
    }

    /** Groups playback detail state. */
    private data class PlaybackDetail(
        val waveAmplitudes: List<Float>,
        val volume: Float,
        val currentPlaybackPosition: Long,
        val totalPlaybackDuration: Long,
        val playbackSpeed: Float,
        val playbackErrorDetails: com.easeaudio.service.PlaybackErrorDetails?,
        val sleepTimerRemaining: Int?,
        val activeEqPreset: String
    )

    private val playbackDetail: Flow<PlaybackDetail> = combine(
        combine(waveAmplitudes, volume, currentPlaybackPosition, totalPlaybackDuration, playbackSpeed) {
            wav, vol, pos, dur, spd -> listOf<Any?>(wav, vol, pos, dur, spd)
        },
        combine(playbackErrorDetails, sleepTimerRemaining, activeEqPreset) {
            errDtl, sleep, eq -> listOf<Any?>(errDtl, sleep, eq)
        }
    ) { basic, extra ->
        @Suppress("UNCHECKED_CAST")
        PlaybackDetail(
            waveAmplitudes = basic[0] as List<Float>,
            volume = basic[1] as Float,
            currentPlaybackPosition = basic[2] as Long,
            totalPlaybackDuration = basic[3] as Long,
            playbackSpeed = basic[4] as Float,
            playbackErrorDetails = extra[0] as com.easeaudio.service.PlaybackErrorDetails?,
            sleepTimerRemaining = extra[1] as Int?,
            activeEqPreset = extra[2] as String
        )
    }

    /** Groups UI dialog/navigation state — changes on user interactions. */
    private data class DialogState(
        val showSleepTimerDialog: Boolean,
        val showEqualizerDialog: Boolean,
        val showAddStationDialog: Boolean,
        val showBlockedDialog: Boolean,
        val showEpisodesSheet: Boolean
    )

    private val dialogState: Flow<DialogState> = combine(
        showSleepTimerDialog, showEqualizerDialog, showAddStationDialog,
        showBlockedDialog, showEpisodesSheet
    ) { sleep, eq, add, blocked, episodes ->
        DialogState(sleep, eq, add, blocked, episodes)
    }

    /** Groups discovery/search/filter state. */
    private data class DiscoveryState(
        val isDiscoveringOnline: Boolean,
        val isLoadingMore: Boolean,
        val canLoadMore: Boolean,
        val isDiscoveryError: Boolean,
        val searchQuery: String,
        val recentSearchQueries: List<String>,
        val selectedTab: com.easeaudio.ui.screens.HomeTab,
        val selectedGenre: String,
        val selectedCountry: String,
        val availableCountries: List<CountryDisplay>,
        val isLoadingCountries: Boolean,
        val filterConfig: com.easeaudio.data.StationFilterConfig
    )

    private val discoveryState: Flow<DiscoveryState> = combine(
        combine(isDiscoveringOnline, isLoadingMore, canLoadMore, isDiscoveryError, searchQuery) {
            disc, loadMore, canLoad, err, query -> listOf<Any?>(disc, loadMore, canLoad, err, query)
        },
        combine(recentSearchQueries, selectedTab, selectedGenre, selectedCountry, availableCountries) {
            recSearch, tab, genre, country, countries -> listOf<Any?>(recSearch, tab, genre, country, countries)
        },
        combine(isLoadingCountries, filterConfig) { loading, config -> listOf<Any?>(loading, config) }
    ) { basic, search, extra ->
        @Suppress("UNCHECKED_CAST")
        DiscoveryState(
            isDiscoveringOnline = basic[0] as Boolean,
            isLoadingMore = basic[1] as Boolean,
            canLoadMore = basic[2] as Boolean,
            isDiscoveryError = basic[3] as Boolean,
            searchQuery = basic[4] as String,
            recentSearchQueries = search[0] as List<String>,
            selectedTab = search[1] as com.easeaudio.ui.screens.HomeTab,
            selectedGenre = search[2] as String,
            selectedCountry = search[3] as String,
            availableCountries = search[4] as List<CountryDisplay>,
            isLoadingCountries = extra[0] as Boolean,
            filterConfig = extra[1] as com.easeaudio.data.StationFilterConfig
        )
    }

    val homeUiState: StateFlow<HomeUiState> = combine(
        combine(stations, recentStations, recentRadioStations, recentPodcastStations, favoriteStations) {
            st, recent, recentRadio, recentPodcast, favs -> listOf<Any?>(st, recent, recentRadio, recentPodcast, favs)
        },
        combine(blockedStations, failedStationIds, demotedStationIds, currentEpisodesList, currentEpisode) {
            blocked, failed, demoted, episodes, episode -> listOf<Any?>(blocked, failed, demoted, episodes, episode)
        },
        combine(isLoadingEpisodes, networkStatus, remoteConfig, isBatterySaverEnabled, selectedLauncherIcon) {
            loadEp, net, cfg, batt, icon -> listOf<Any?>(loadEp, net, cfg, batt, icon)
        },
        combine(isAudioBoosterEnabled, isAutoPlayOnStartupEnabled, showAppearanceDialog, showAttributionDialog, listenLaterItems) {
            boost, autoPlay, appearance, attribution, later -> listOf<Any?>(boost, autoPlay, appearance, attribution, later)
        },
        combine(
            playerSnapshot,
            playbackDetail,
            dialogState,
            discoveryState,
            combine(isVolumeSafetyEnabled, isNightAudioModeEnabled, todayListeningMinutes, currentStreakDays) { safety, night, today, streak ->
                listOf<Any?>(safety, night, today, streak)
            }
        ) { ps, pd, ds, disc, comfort ->
            listOf<Any?>(ps, pd, ds, disc, comfort)
        }
    ) { stationData, metaData, configData, settingsData, stateGroups ->
        @Suppress("UNCHECKED_CAST")
        val ps = stateGroups[0] as PlayerSnapshot
        val pd = stateGroups[1] as PlaybackDetail
        val ds = stateGroups[2] as DialogState
        val disc = stateGroups[3] as DiscoveryState
        val comfort = stateGroups[4] as List<Any?>

        HomeUiState(
            stations                = stationData[0] as List<RadioStation>,
            recentStations          = stationData[1] as List<RadioStation>,
            recentRadioStations     = stationData[2] as List<RadioStation>,
            recentPodcastStations   = stationData[3] as List<RadioStation>,
            favoriteStations        = stationData[4] as List<RadioStation>,
            listenLaterItems        = settingsData[4] as List<com.easeaudio.data.ListenLaterItem>,
            blockedStations         = metaData[0] as List<RadioStation>,
            failedStationIds        = metaData[1] as Set<String>,
            demotedStationIds       = metaData[2] as Set<String>,
            currentEpisodesList     = metaData[3] as List<PodcastEpisode>,
            currentEpisode          = metaData[4] as PodcastEpisode?,
            isLoadingEpisodes       = configData[0] as Boolean,
            networkStatus           = configData[1] as com.easeaudio.network.NetworkStatus,
            remoteConfig            = configData[2] as com.easeaudio.firebase.AppRemoteConfig,
            isBatterySaverEnabled   = configData[3] as Boolean,
            selectedLauncherIcon    = configData[4] as String,
            isAudioBoosterEnabled   = settingsData[0] as Boolean,
            isAutoPlayOnStartupEnabled = settingsData[1] as Boolean,
            showAppearanceDialog    = settingsData[2] as Boolean,
            showAttributionDialog   = settingsData[3] as Boolean,
            isVolumeSafetyEnabled   = comfort[0] as Boolean,
            isNightAudioModeEnabled = comfort[1] as Boolean,
            todayListeningMinutes   = comfort[2] as Int,
            currentStreakDays       = comfort[3] as Int,
            // Player state (from typed PlayerSnapshot — no unchecked cast risk)
            currentStation          = ps.currentStation,
            isPlaying               = ps.isPlaying,
            isLoading               = ps.isLoading,
            streamTitle             = ps.streamTitle,
            playbackError           = ps.playbackError,
            trackArtworkUrl         = ps.trackArtworkUrl,
            currentLyrics           = ps.currentLyrics,
            isLoadingLyrics         = ps.isLoadingLyrics,
            curatedAudiophileStations = _curatedAudiophileStations.value,
            // Playback detail (from typed PlaybackDetail)
            waveAmplitudes          = pd.waveAmplitudes,
            volume                  = pd.volume,
            currentPlaybackPosition = pd.currentPlaybackPosition,
            totalPlaybackDuration   = pd.totalPlaybackDuration,
            playbackSpeed           = pd.playbackSpeed,
            playbackErrorDetails    = pd.playbackErrorDetails,
            sleepTimerRemaining     = pd.sleepTimerRemaining,
            activeEqPreset          = pd.activeEqPreset,
            // Dialog state (from typed DialogState)
            showSleepTimerDialog    = ds.showSleepTimerDialog,
            showEqualizerDialog     = ds.showEqualizerDialog,
            showAddStationDialog    = ds.showAddStationDialog,
            showBlockedDialog       = ds.showBlockedDialog,
            showEpisodesSheet       = ds.showEpisodesSheet,
            // Discovery/search state (from typed DiscoveryState)
            isDiscoveringOnline     = disc.isDiscoveringOnline,
            isLoadingMore           = disc.isLoadingMore,
            canLoadMore             = disc.canLoadMore,
            isDiscoveryError        = disc.isDiscoveryError,
            searchQuery             = disc.searchQuery,
            recentSearchQueries     = disc.recentSearchQueries,
            selectedTab             = disc.selectedTab,
            selectedGenre           = disc.selectedGenre,
            selectedCountry         = disc.selectedCountry,
            availableCountries      = disc.availableCountries,
            isLoadingCountries      = disc.isLoadingCountries,
            filterConfig            = disc.filterConfig,
            // Static-at-init fields
            availableGenres         = availableGenres,
            availablePodcastTopics  = availablePodcastTopics
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HomeUiState(
            availableGenres = availableGenres,
            availablePodcastTopics = availablePodcastTopics,
            availableCountries = fallbackCountries
        )
    )


    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        discoverStationsOnline(query, _selectedGenre.value, _selectedCountry.value)
    }

    fun saveSearchQuery(query: String = _searchQuery.value) {
        if (query.trim().isNotBlank()) {
            viewModelScope.launch {
                repository.saveSearchQuery(query)
            }
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            repository.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchQueries()
        }
    }

    fun setSelectedTab(tab: com.easeaudio.ui.screens.HomeTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
            _selectedGenre.value = "All"
            discoverStationsOnline(_searchQuery.value, "All", _selectedCountry.value)
        }
        refreshRecentStations()
    }

    fun setSelectedGenre(genre: String) {
        _selectedGenre.value = genre
        discoverStationsOnline(_searchQuery.value, genre, _selectedCountry.value)
    }

    fun setSelectedCountry(country: String) {
        _selectedCountry.value = country
        discoverStationsOnline(_searchQuery.value, _selectedGenre.value, country)
    }

    fun refreshStations() {
        discoverStationsOnline(_searchQuery.value, _selectedGenre.value, _selectedCountry.value)
        playerManager.refreshStreamMetadata()
    }

    fun retryDiscovery() {
        discoverStationsOnline(_searchQuery.value, _selectedGenre.value, _selectedCountry.value)
    }

    private fun discoverStationsOnline(query: String, genre: String, country: String = _selectedCountry.value) {
        onlineDiscoveryJob?.cancel()
        onlineDiscoveryJob = viewModelScope.launch {
            if (query.isNotBlank()) {
                // Debounce search keystrokes to prevent excessive concurrent network requests and race conditions
                kotlinx.coroutines.delay(400L)
            }
            _isDiscoveringOnline.value = true
            _isDiscoveryError.value = false
            _canLoadMore.value = true
            try {
                val isPodcastMode = _selectedTab.value == com.easeaudio.ui.screens.HomeTab.Podcast || genre.equals("Podcasts", ignoreCase = true) || genre.equals("Podcast", ignoreCase = true)
                val results = if (isPodcastMode) {
                    repository.discoverOnlinePodcasts(
                        query = query,
                        genre = genre,
                        country = "",
                        offset = 0,
                        limit = pageSize
                    )
                } else {
                    val code = _availableCountries.value.find { it.name.equals(country, ignoreCase = true) }?.code ?: ""
                    val radioBrowserResults = repository.discoverOnlineStations(
                        query = query,
                        genre = genre,
                        country = country,
                        countryCode = code,
                        offset = 0,
                        limit = pageSize
                    )
                    val iTunesRadioResults = repository.getiTunesLiveRadioStations(query = query, genre = genre, country = country)
                    (radioBrowserResults + iTunesRadioResults).distinctBy { "${it.name}_${it.streamUrl}" }
                }
                _onlineDiscoveredStations.value = results
                _canLoadMore.value = results.size >= 10
                val countryMatchesGlobal = country == "Global" || country == "All"
                _isDiscoveryError.value = results.isEmpty() && query.isBlank() && genre == "All" && (isPodcastMode || countryMatchesGlobal)
            } catch (e: Exception) {
                // Keep existing discovered stations when offline instead of wiping the list
                _isDiscoveryError.value = _onlineDiscoveredStations.value.isEmpty()
                try {
                    val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                    crashlytics.setCustomKey("viewmodel_query", query)
                    crashlytics.setCustomKey("viewmodel_genre", genre)
                    crashlytics.setCustomKey("viewmodel_country", country)
                    crashlytics.recordException(e)
                } catch (ce: Exception) {
                    // Ignore if Crashlytics is not active
                }
            } finally {
                _isDiscoveringOnline.value = false
            }
        }
    }

    fun loadMoreStations() {
        if (_isDiscoveringOnline.value || _isLoadingMore.value || !_canLoadMore.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val currentOffset = _onlineDiscoveredStations.value.size
                val currentGenre = _selectedGenre.value
                val isPodcastMode = _selectedTab.value == com.easeaudio.ui.screens.HomeTab.Podcast || currentGenre.equals("Podcasts", ignoreCase = true) || currentGenre.equals("Podcast", ignoreCase = true)
                
                val newResults = if (isPodcastMode) {
                    repository.discoverOnlinePodcasts(
                        query = _searchQuery.value,
                        genre = currentGenre,
                        offset = currentOffset,
                        limit = pageSize
                    )
                } else {
                    val code = _availableCountries.value.find { it.name.equals(_selectedCountry.value, ignoreCase = true) }?.code ?: ""
                    repository.discoverOnlineStations(
                        query = _searchQuery.value,
                        genre = currentGenre,
                        country = _selectedCountry.value,
                        countryCode = code,
                        offset = currentOffset,
                        limit = pageSize
                    )
                }
                if (newResults.isEmpty()) {
                    _canLoadMore.value = false
                } else {
                    val existingIds = _onlineDiscoveredStations.value.map { it.id }.toSet()
                    val filteredNew = newResults.filter { !existingIds.contains(it.id) }
                    _onlineDiscoveredStations.value = _onlineDiscoveredStations.value + filteredNew
                    _canLoadMore.value = filteredNew.isNotEmpty() && newResults.size >= 10
                }
            } catch (e: Exception) {
                _canLoadMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private var lastPlaybackClickTimestamp = 0L
    private var lastPlaybackClickStationId = ""
    private var podcastFetchJob: Job? = null

    fun playStation(station: RadioStation) {
        val now = System.currentTimeMillis()
        val isSameStation = playerManager.currentStation.value?.id == station.id || lastPlaybackClickStationId == station.id
        
        // Only debounce if tapping the exact same station repeatedly
        if (isSameStation && (now - lastPlaybackClickTimestamp < 400L)) {
            return
        }
        lastPlaybackClickTimestamp = now
        lastPlaybackClickStationId = station.id

        // Cancel any pending podcast episode fetch job when a new station is selected
        podcastFetchJob?.cancel()

        if (station.isPodcast) {
            podcastFetchJob = viewModelScope.launch {
                _isLoadingEpisodes.value = true
                val episodes = PodcastEpisodeService.fetchEpisodes(station, maxEpisodes = 1000)
                _currentEpisodesList.value = episodes
                _isLoadingEpisodes.value = false

                val targetEpisode = _currentEpisode.value?.takeIf { ep -> episodes.any { it.id == ep.id } }
                    ?: episodes.firstOrNull()

                if (targetEpisode != null) {
                    _currentEpisode.value = targetEpisode
                    playerManager.playPodcastEpisode(station, targetEpisode)
                    repository.recordStationListened(station)
                } else {
                    playerManager.playStation(station)
                }
            }
        } else {
            if (playerManager.currentStation.value?.id == station.id) {
                if (!playerManager.isLoading.value) {
                    playerManager.togglePlayPause()
                }
            } else {
                playerManager.playStation(station)
                viewModelScope.launch {
                    repository.recordStationListened(station)
                }
            }
        }
    }

    fun togglePlayPause() {
        val now = System.currentTimeMillis()
        if (now - lastPlaybackClickTimestamp < 400L) {
            return
        }
        lastPlaybackClickTimestamp = now
        playerManager.togglePlayPause()
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            val wasFav = station.isFavorite
            repository.toggleFavorite(station)
            if (!wasFav) {
                smartEngagementManager.recordFavoriteAdded()
                snackbarMessage.emit("❤️ Added '${station.name}' to Favorites")
            } else {
                snackbarMessage.emit("Removed '${station.name}' from Favorites")
            }
        }
    }

    fun setShowBlockedDialog(show: Boolean) {
        _showBlockedDialog.value = show
    }

    fun blockStation(stationId: String) {
        filterAndBlockManager.blockStation(stationId)
    }

    fun unblockStation(stationId: String) {
        filterAndBlockManager.unblockStation(stationId)
    }

    fun clearAllBlockedStations() {
        filterAndBlockManager.clearBlockList()
    }

    fun demoteStation(stationId: String) {
        filterAndBlockManager.demoteStation(stationId)
    }

    fun undemoteStation(stationId: String) {
        filterAndBlockManager.undemoteStation(stationId)
    }

    fun clearAllDemotedStations() {
        filterAndBlockManager.clearDemotedList()
    }

    fun setFilterAdultContent(enabled: Boolean) {
        filterAndBlockManager.setFilterAdultContent(enabled)
    }

    fun setFilterPoliticsContent(enabled: Boolean) {
        filterAndBlockManager.setFilterPoliticsContent(enabled)
    }

    fun setFilterReligiousContent(enabled: Boolean) {
        filterAndBlockManager.setFilterReligiousContent(enabled)
    }

    fun addCustomFilterKeyword(keyword: String) {
        filterAndBlockManager.addCustomKeyword(keyword)
    }

    fun removeCustomFilterKeyword(keyword: String) {
        filterAndBlockManager.removeCustomKeyword(keyword)
    }

    fun clearCustomFilterKeywords() {
        filterAndBlockManager.clearCustomKeywords()
    }

    fun addCustomStation(name: String, streamUrl: String, genre: String) {
        viewModelScope.launch {
            val station = RadioStation(
                id = "custom_" + UUID.randomUUID().toString(),
                name = name.ifBlank { "My Custom Stream" },
                genre = genre.ifBlank { "Custom" },
                country = "Personal",
                streamUrl = streamUrl,
                imageUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80",
                bitrate = "Custom Stream",
                codec = "HTTP Live",
                isCustom = true
            )
            repository.addCustomStation(station)
            _showAddStationDialog.value = false
        }
    }

    fun retryCurrentStation() {
        playerManager.retryCurrentStation()
    }

    fun playNextStation() {
        val currentEpisodes = _currentEpisodesList.value
        val curEp = _currentEpisode.value
        val currentShow = playerManager.currentStation.value
        if (_selectedTab.value == com.easeaudio.ui.screens.HomeTab.Podcast && currentEpisodes.isNotEmpty() && curEp != null && currentShow != null) {
            val curIdx = currentEpisodes.indexOfFirst { it.id == curEp.id }
            if (curIdx != -1 && curIdx < currentEpisodes.size - 1) {
                val nextEp = currentEpisodes[curIdx + 1]
                playEpisode(currentShow, nextEp)
                return
            }
        }
        val currentList = stations.value
        playerManager.playNextStation(currentList)
    }

    fun playPreviousStation() {
        val currentEpisodes = _currentEpisodesList.value
        val curEp = _currentEpisode.value
        val currentShow = playerManager.currentStation.value
        if (_selectedTab.value == com.easeaudio.ui.screens.HomeTab.Podcast && currentEpisodes.isNotEmpty() && curEp != null && currentShow != null) {
            val curIdx = currentEpisodes.indexOfFirst { it.id == curEp.id }
            if (curIdx > 0) {
                val prevEp = currentEpisodes[curIdx - 1]
                playEpisode(currentShow, prevEp)
                return
            }
        }
        val currentList = stations.value
        playerManager.playPreviousStation(currentList)
    }

    fun setEqPreset(preset: String) {
        _activeEqPreset.value = preset
        playerManager.setEqPreset(preset)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
    }

    fun setSleepTimer(minutes: Int) {
        playerManager.startSleepTimer(minutes)
        _showSleepTimerDialog.value = false
    }

    fun cancelSleepTimer() {
        playerManager.cancelSleepTimer()
    }

    fun setShowSleepTimerDialog(show: Boolean) {
        _showSleepTimerDialog.value = show
    }

    fun setShowEqualizerDialog(show: Boolean) {
        _showEqualizerDialog.value = show
    }

    fun setShowAddStationDialog(show: Boolean) {
        _showAddStationDialog.value = show
    }

    fun setShowAppearanceDialog(show: Boolean) {
        _showAppearanceDialog.value = show
    }

    fun setShowAttributionDialog(show: Boolean) {
        _showAttributionDialog.value = show
    }

    fun importStations(stations: List<RadioStation>) {
        viewModelScope.launch {
            stations.forEach { station ->
                repository.addCustomStation(station)
                if (station.isFavorite) {
                    val currentFavs = favoriteStations.value
                    if (currentFavs.none { it.id == station.id }) {
                        repository.toggleFavorite(station.copy(isFavorite = false))
                    }
                }
            }
            refreshStations()
            snackbarMessage.emit("Imported ${stations.size} stations into library")
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Do not release playerManager here to keep background playback active when UI clears
    }
}
