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

    @OptIn(FlowPreview::class)
    val stations: StateFlow<List<RadioStation>> = combine(
        repository.getAllStations(),
        _onlineDiscoveredStations,
        _searchQuery,
        _selectedGenre
    ) { localStations, onlineList, query, genre ->
        // Merge local curated & custom with online discovered stations
        val favMap = localStations.filter { it.isFavorite }.associateBy { it.id }
        
        val mergedList = mutableListOf<RadioStation>()
        val addedIds = mutableSetOf<String>()

        // Add local/curated first
        localStations.forEach {
            mergedList.add(it)
            addedIds.add(it.id)
        }

        // Add online discovered if not duplicate
        onlineList.forEach { online ->
            if (!addedIds.contains(online.id)) {
                val isFav = favMap.containsKey(online.id)
                mergedList.add(online.copy(isFavorite = isFav))
                addedIds.add(online.id)
            }
        }

        // Filter merged results
        mergedList.filter { station ->
            val matchesQuery = query.isBlank() ||
                    station.name.contains(query, ignoreCase = true) ||
                    station.genre.contains(query, ignoreCase = true) ||
                    station.country.contains(query, ignoreCase = true)

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
                "Classical" -> station.genre.contains("Classic", ignoreCase = true) || station.genre.contains("Piano", ignoreCase = true)
                "Ambient" -> station.genre.contains("Ambient", ignoreCase = true) || station.genre.contains("Drone", ignoreCase = true)
                "EDM" -> station.genre.contains("EDM", ignoreCase = true) || station.genre.contains("Dance", ignoreCase = true) || station.genre.contains("House", ignoreCase = true) || station.genre.contains("Techno", ignoreCase = true)
                else -> station.genre.contains(genre, ignoreCase = true)
            }

            matchesQuery && matchesGenre
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
        // Trigger initial online discovery for top global working stations
        discoverStationsOnline("", "All")

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
        discoverStationsOnline(query, _selectedGenre.value)
    }

    fun setSelectedGenre(genre: String) {
        _selectedGenre.value = genre
        discoverStationsOnline(_searchQuery.value, genre)
    }

    fun refreshStations() {
        discoverStationsOnline(_searchQuery.value, _selectedGenre.value)
    }

    fun retryDiscovery() {
        discoverStationsOnline(_searchQuery.value, _selectedGenre.value)
    }

    private fun discoverStationsOnline(query: String, genre: String) {
        viewModelScope.launch {
            _isDiscoveringOnline.value = true
            _isDiscoveryError.value = false
            _canLoadMore.value = true
            try {
                val results = repository.discoverOnlineStations(
                    query = query,
                    genre = genre,
                    offset = 0,
                    limit = pageSize
                )
                _onlineDiscoveredStations.value = results
                _canLoadMore.value = results.size >= pageSize
                _isDiscoveryError.value = results.isEmpty() && query.isBlank() && genre == "All"
            } catch (e: Exception) {
                _isDiscoveryError.value = _onlineDiscoveredStations.value.isEmpty()
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
                val newResults = repository.discoverOnlineStations(
                    query = _searchQuery.value,
                    genre = _selectedGenre.value,
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
        playerManager.playStation(station)
        viewModelScope.launch {
            repository.recordStationListened(station)
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
