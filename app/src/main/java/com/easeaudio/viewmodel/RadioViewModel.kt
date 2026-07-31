package com.easeaudio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.easeaudio.data.RadioDatabase
import com.easeaudio.data.RadioRepository
import com.easeaudio.data.RadioStation
import com.easeaudio.service.RadioPlayerManager
import com.easeaudio.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    val repository: RadioRepository = RadioRepository(RadioDatabase.getDatabase(application).radioDao())
    val playerManager: RadioPlayerManager = RadioPlayerManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

    private val _selectedCountry = MutableStateFlow("Global")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    // ---- Country Discovery ----

    /** Fallback countries shown instantly while the API call loads (sorted by station count desc) */
    private val fallbackCountries = listOf(
        CountryDisplay("Global",         "🌐", "",   50000, "> 50k stns"),
        CountryDisplay("United States",  "🇺🇸", "US", 5000,  "> 5,000 stns"),
        CountryDisplay("Germany",        "🇩🇪", "DE", 3000,  "> 3,000 stns"),
        CountryDisplay("United Kingdom", "🇬🇧", "GB", 2000,  "> 2,000 stns"),
        CountryDisplay("France",         "🇫🇷", "FR", 2000,  "> 2,000 stns"),
        CountryDisplay("Spain",          "🇪🇸", "ES", 1000,  "> 1,000 stns"),
        CountryDisplay("Italy",          "🇮🇹", "IT", 1000,  "> 1,000 stns"),
        CountryDisplay("Canada",         "🇨🇦", "CA", 800,   "> 800 stns"),
        CountryDisplay("Brazil",         "🇧🇷", "BR", 800,   "> 800 stns"),
        CountryDisplay("Australia",      "🇦🇺", "AU", 500,   "> 500 stns"),
        CountryDisplay("Mexico",         "🇲🇽", "MX", 500,   "> 500 stns"),
        CountryDisplay("Argentina",      "🇦🇷", "AR", 400,   "> 400 stns"),
        CountryDisplay("Netherlands",    "🇳🇱", "NL", 400,   "> 400 stns"),
        CountryDisplay("Poland",         "🇵🇱", "PL", 400,   "> 400 stns"),
        CountryDisplay("Ukraine",        "🇺🇦", "UA", 300,   "> 300 stns"),
        CountryDisplay("Switzerland",    "🇨🇭", "CH", 300,   "> 300 stns"),
        CountryDisplay("Austria",        "🇦🇹", "AT", 300,   "> 300 stns"),
        CountryDisplay("Belgium",        "🇧🇪", "BE", 300,   "> 300 stns"),
        CountryDisplay("Turkey",         "🇹🇷", "TR", 200,   "> 200 stns"),
        CountryDisplay("Sweden",         "🇸🇪", "SE", 200,   "> 200 stns"),
        CountryDisplay("Japan",          "🇯🇵", "JP", 200,   "> 200 stns"),
        CountryDisplay("India",          "🇮🇳", "IN", 200,   "> 200 stns"),
        CountryDisplay("Norway",         "🇳🇴", "NO", 150,   "> 150 stns"),
        CountryDisplay("Czech Republic", "🇨🇿", "CZ", 150,   "> 150 stns"),
        CountryDisplay("Portugal",       "🇵🇹", "PT", 150,   "> 150 stns"),
        CountryDisplay("Greece",         "🇬🇷", "GR", 150,   "> 150 stns"),
        CountryDisplay("Ireland",        "🇮🇪", "IE", 100,   "> 100 stns"),
        CountryDisplay("Chile",          "🇨🇱", "CL", 100,   "> 100 stns"),
        CountryDisplay("Colombia",       "🇨🇴", "CO", 100,   "> 100 stns"),
        CountryDisplay("South Africa",   "🇿🇦", "ZA", 100,   "> 100 stns"),
        CountryDisplay("New Zealand",    "🇳🇿", "NZ", 80,    "> 80 stns"),
        CountryDisplay("Vietnam",        "🇻🇳", "VN", 50,    "> 50 stns"),
        CountryDisplay("Indonesia",      "🇮🇩", "ID", 50,    "> 50 stns"),
        CountryDisplay("Thailand",       "🇹🇭", "TH", 40,    "> 40 stns"),
        CountryDisplay("Philippines",    "🇵🇭", "PH", 30,    "> 30 stns"),
        CountryDisplay("Malaysia",       "🇲🇾", "MY", 30,    "> 30 stns"),
        CountryDisplay("Singapore",      "🇸🇬", "SG", 20,    "~20 stns"),
        CountryDisplay("South Korea",    "🇰🇷", "KR", 20,    "~20 stns"),
        CountryDisplay("Nigeria",        "🇳🇬", "NG", 20,    "~20 stns"),
        CountryDisplay("Egypt",          "🇪🇬", "EG", 15,    "~15 stns")
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
                    stationCountText = "> 50k stns"
                )

                // Convert API triples -> CountryDisplay, sorted by stationCount descending
                val discovered = apiData
                    .map { (name, isoCode, count) ->
                        val flag = com.easeaudio.data.RadioBrowserService.isoToFlagEmoji(isoCode)
                        val countText = when {
                            count >= 1000 -> "${"%,d".format(count)} stns"
                            count >= 100  -> "$count stns"
                            else          -> "~$count stns"
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
        GenreDisplay("News & Reports", R.string.news_reports),
        GenreDisplay("Lo-Fi & Chill", R.string.lofi_chill),
        GenreDisplay("Pop", R.string.pop),
        GenreDisplay("Jazz", R.string.jazz),
        GenreDisplay("Rock", R.string.rock),
        GenreDisplay("Hip Hop", R.string.hip_hop),
        GenreDisplay("Classical", R.string.classical),
        GenreDisplay("Ambient", R.string.ambient),
        GenreDisplay("EDM", R.string.edm),
        GenreDisplay("House", R.string.house),
        GenreDisplay("Country", R.string.country_genre),
        GenreDisplay("Custom", R.string.custom),
    )

    val availableEqPresets = listOf(
        EqPresetDisplay("Balanced", R.string.balanced),
        EqPresetDisplay("Speech", R.string.dsp_speech),
        EqPresetDisplay("LoFi", R.string.dsp_lofi),
        EqPresetDisplay("Acoustic", R.string.dsp_acoustic),
        EqPresetDisplay("Bass", R.string.dsp_bass)
    )

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

    private data class FilterState(
        val query: String,
        val genre: String,
        val country: String,
        val preferredGenres: Set<String>
    )

    private val defaultStationIds = setOf("bbc_world_service", "jazz_groove", "lofi_girl_radio")

    @OptIn(FlowPreview::class)
    val stations: StateFlow<List<RadioStation>> = combine(
        repository.getAllStations(),
        _onlineDiscoveredStations,
        combine(_searchQuery, _selectedGenre, _selectedCountry, _preferredGenres) { query, genre, country, preferred ->
            FilterState(query, genre, country, preferred)
        }
    ) { localStations, onlineList, filter ->
        val query = filter.query
        val genre = filter.genre
        val country = filter.country
        val preferredGenres = filter.preferredGenres

        val localMap = localStations.associateBy { it.id }
        
        val mergedList = mutableListOf<RadioStation>()
        val addedIds = mutableSetOf<String>()

        // 1. Add custom stations created by user first
        val customStations = localStations.filter { it.isCustom }
        customStations.forEach { station ->
            mergedList.add(station)
            addedIds.add(station.id)
        }

        // 2. Add online discovered stations in their stable discovered order, merging local state (isFavorite, etc.)
        onlineList.forEach { online ->
            if (!addedIds.contains(online.id)) {
                val localCopy = localMap[online.id]
                val mergedStation = online.copy(
                    isFavorite = localCopy?.isFavorite ?: online.isFavorite,
                    isCustom = localCopy?.isCustom ?: online.isCustom,
                    lastListenedTimestamp = localCopy?.lastListenedTimestamp ?: online.lastListenedTimestamp
                )
                mergedList.add(mergedStation)
                addedIds.add(online.id)
            }
        }

        // 3. Add default curated stations and other cached local stations that were not in onlineList
        localStations.forEach { local ->
            if (!addedIds.contains(local.id)) {
                mergedList.add(local)
                addedIds.add(local.id)
            }
        }

        // Filter merged results
        val filteredList = mergedList.filter { station ->
            val matchesQuery = query.isBlank() ||
                    station.name.contains(query, ignoreCase = true) ||
                    station.genre.contains(query, ignoreCase = true) ||
                    station.country.contains(query, ignoreCase = true)

            val isOnlineMatch = onlineList.any { it.id == station.id }

            val matchesCountry = when (country) {
                "Global", "All" -> true
                else -> station.country.contains(country, ignoreCase = true) || isOnlineMatch
            }

            val matchesGenre = when (genre) {
                "All" -> true
                "Custom" -> station.isCustom
                "News & Reports" -> station.genre.contains("News", ignoreCase = true) ||
                        station.genre.contains("Report", ignoreCase = true) ||
                        station.genre.contains("Talk", ignoreCase = true) ||
                        station.genre.contains("Info", ignoreCase = true) ||
                        station.genre.contains("Politic", ignoreCase = true) ||
                        station.genre.contains("Speech", ignoreCase = true)
                "Lo-Fi & Chill" -> station.genre.contains("Lo-Fi", ignoreCase = true) ||
                        station.genre.contains("Chill", ignoreCase = true) ||
                        station.genre.contains("Lofi", ignoreCase = true) ||
                        station.genre.contains("Lounge", ignoreCase = true) ||
                        station.genre.contains("Ambient", ignoreCase = true)
                "Jazz" -> station.genre.contains("Jazz", ignoreCase = true)
                "Rock" -> station.genre.contains("Rock", ignoreCase = true)
                "Classical" -> station.genre.contains("Classic", ignoreCase = true) || station.genre.contains("Piano", ignoreCase = true) || station.genre.contains("Orchestra", ignoreCase = true) || station.genre.contains("Symphony", ignoreCase = true)
                "Ambient" -> station.genre.contains("Ambient", ignoreCase = true) || station.genre.contains("Drone", ignoreCase = true)
                "EDM" -> station.genre.contains("EDM", ignoreCase = true) || station.genre.contains("Dance", ignoreCase = true) || station.genre.contains("House", ignoreCase = true) || station.genre.contains("Techno", ignoreCase = true) || station.genre.contains("Club", ignoreCase = true)
                "House" -> station.genre.contains("House", ignoreCase = true) || station.genre.contains("Deep", ignoreCase = true)
                "Pop" -> station.genre.contains("Pop", ignoreCase = true) || station.genre.contains("Hit", ignoreCase = true) || station.genre.contains("Top 40", ignoreCase = true) || station.genre.contains("Top40", ignoreCase = true)
                "Hip Hop" -> station.genre.contains("Hip", ignoreCase = true) || station.genre.contains("Rap", ignoreCase = true) || station.genre.contains("Urban", ignoreCase = true)
                "Country" -> station.genre.contains("Country", ignoreCase = true) || station.genre.contains("Folk", ignoreCase = true)
                else -> station.genre.contains(genre, ignoreCase = true)
            }

            matchesQuery && matchesCountry && (matchesGenre || isOnlineMatch)
        }

        if (genre == "All" && preferredGenres.isNotEmpty()) {
            filteredList.sortedByDescending { station ->
                val matchesPref = preferredGenres.any { pref ->
                    station.genre.contains(pref, ignoreCase = true) ||
                            (pref.contains("Lo-Fi", ignoreCase = true) && (station.genre.contains("Lo-Fi", ignoreCase = true) || station.genre.contains("Chill", ignoreCase = true) || station.genre.contains("Lofi", ignoreCase = true))) ||
                            (pref.contains("News", ignoreCase = true) && (station.genre.contains("News", ignoreCase = true) || station.genre.contains("Talk", ignoreCase = true)))
                }
                if (matchesPref) 1 else 0
            }
        } else {
            filteredList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    init {
        // Discover countries from RadioBrowser API (fallback shown immediately)
        discoverCountries()
        // Trigger initial online discovery for top global working stations
        discoverStationsOnline("", "All", "Global")

        viewModelScope.launch {
            stations.collect { list ->
                playerManager.updateStationList(list)
            }
        }
    }

    val favoriteStations: StateFlow<List<RadioStation>> = repository.getFavoriteStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStations: StateFlow<List<RadioStation>> = repository.getRecentStations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        discoverStationsOnline(query, _selectedGenre.value, _selectedCountry.value)
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
    }

    fun retryDiscovery() {
        discoverStationsOnline(_searchQuery.value, _selectedGenre.value, _selectedCountry.value)
    }

    private fun discoverStationsOnline(query: String, genre: String, country: String = _selectedCountry.value) {
        viewModelScope.launch {
            _isDiscoveringOnline.value = true
            _isDiscoveryError.value = false
            _canLoadMore.value = true
            try {
                val code = _availableCountries.value.find { it.name.equals(country, ignoreCase = true) }?.code ?: ""
                val results = repository.discoverOnlineStations(
                    query = query,
                    genre = genre,
                    country = country,
                    countryCode = code,
                    offset = 0,
                    limit = pageSize
                )
                _onlineDiscoveredStations.value = results
                _canLoadMore.value = results.size >= pageSize
                _isDiscoveryError.value = results.isEmpty() && query.isBlank() && genre == "All" && (country == "Global" || country == "All")
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
                val code = _availableCountries.value.find { it.name.equals(_selectedCountry.value, ignoreCase = true) }?.code ?: ""
                val newResults = repository.discoverOnlineStations(
                    query = _searchQuery.value,
                    genre = _selectedGenre.value,
                    country = _selectedCountry.value,
                    countryCode = code,
                    offset = currentOffset,
                    limit = pageSize
                )
                if (newResults.isEmpty()) {
                    _canLoadMore.value = false
                } else {
                    val existingIds = _onlineDiscoveredStations.value.map { it.id }.toSet()
                    val filteredNew = newResults.filter { !existingIds.contains(it.id) }
                    _onlineDiscoveredStations.value = _onlineDiscoveredStations.value + filteredNew
                    _canLoadMore.value = newResults.size >= pageSize
                }
            } catch (e: Exception) {
                _canLoadMore.value = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun playStation(station: RadioStation) {
        if (playerManager.currentStation.value?.id == station.id) {
            playerManager.togglePlayPause()
        } else {
            playerManager.playStation(station)
            viewModelScope.launch {
                repository.recordStationListened(station)
            }
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun toggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            repository.toggleFavorite(station)
        }
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

    val networkStatus = playerManager.networkStatus
    val remoteConfig = playerManager.remoteConfig
    val failedStationIds = playerManager.failedStationIds
    val playbackError = playerManager.playbackError

    fun retryCurrentStation() {
        playerManager.retryCurrentStation()
    }

    fun playNextStation() {
        val currentList = stations.value
        playerManager.playNextStation(currentList)
    }

    fun playPreviousStation() {
        val currentList = stations.value
        playerManager.playPreviousStation(currentList)
    }

    fun toggleSimulatedAds(enabled: Boolean) {
        playerManager.firebaseConfigManager.toggleSimulatedAds(enabled)
    }

    fun setEqPreset(preset: String) {
        _activeEqPreset.value = preset
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

    override fun onCleared() {
        super.onCleared()
        // Do not release playerManager here to keep background playback active when UI clears
    }
}
