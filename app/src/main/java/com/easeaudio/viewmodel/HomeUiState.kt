package com.easeaudio.viewmodel

import com.easeaudio.data.RadioStation
import com.easeaudio.data.PodcastEpisode
import com.easeaudio.data.StationFilterConfig
import com.easeaudio.network.NetworkStatus
import com.easeaudio.firebase.AppRemoteConfig
import com.easeaudio.service.PlaybackErrorDetails

data class HomeUiState(
    // Data Lists
    val stations: List<RadioStation> = emptyList(),
    val recentStations: List<RadioStation> = emptyList(),
    val recentRadioStations: List<RadioStation> = emptyList(),
    val recentPodcastStations: List<RadioStation> = emptyList(),
    val favoriteStations: List<RadioStation> = emptyList(),
    val blockedStations: List<RadioStation> = emptyList(),
    val demotedStationIds: Set<String> = emptySet(),
    val failedStationIds: Set<String> = emptySet(),
    
    // Playback State
    val currentStation: RadioStation? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val streamTitle: String? = null,
    val trackArtworkUrl: String? = null,
    val currentLyrics: com.easeaudio.data.SongLyrics? = null,
    val isLoadingLyrics: Boolean = false,
    val curatedAudiophileStations: List<RadioStation> = emptyList(),
    val waveAmplitudes: List<Float> = List(8) { 0.15f },
    val volume: Float = 0.85f,
    val currentPlaybackPosition: Long = 0L,
    val totalPlaybackDuration: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val playbackError: String? = null,
    val playbackErrorDetails: PlaybackErrorDetails? = null,
    
    // Discovery & Filter State
    val isDiscoveringOnline: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val isDiscoveryError: Boolean = false,
    val searchQuery: String = "",
    val recentSearchQueries: List<String> = emptyList(),
    val selectedTab: com.easeaudio.ui.screens.HomeTab = com.easeaudio.ui.screens.HomeTab.Radio,
    val selectedGenre: String = "All",
    val selectedCountry: String = "Global",
    val availableGenres: List<GenreDisplay> = emptyList(),
    val availablePodcastTopics: List<GenreDisplay> = emptyList(),
    val availableCountries: List<CountryDisplay> = emptyList(),
    val isLoadingCountries: Boolean = false,
    val filterConfig: StationFilterConfig = StationFilterConfig(),
    
    // Podcast State
    val currentEpisodesList: List<PodcastEpisode> = emptyList(),
    val currentEpisode: PodcastEpisode? = null,
    val isLoadingEpisodes: Boolean = false,
    
    // System & Settings
    val sleepTimerRemaining: Int? = null,
    val networkStatus: NetworkStatus = NetworkStatus(),
    val remoteConfig: AppRemoteConfig = AppRemoteConfig(),
    val activeEqPreset: String = "Balanced",
    val isAudioBoosterEnabled: Boolean = true,
    val isBatterySaverEnabled: Boolean = false,
    val isAutoPlayOnStartupEnabled: Boolean = true,
    val selectedLauncherIcon: String = "default",
    
    // UI Dialogs
    val showSleepTimerDialog: Boolean = false,
    val showEqualizerDialog: Boolean = false,
    val showAddStationDialog: Boolean = false,
    val showBlockedDialog: Boolean = false,
    val showEpisodesSheet: Boolean = false,
    val showAppearanceDialog: Boolean = false,
    val showAttributionDialog: Boolean = false
)
