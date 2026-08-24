package com.easeaudio.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.easeaudio.R
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.easeaudio.data.RadioDatabase
import com.easeaudio.data.RadioStation
import com.easeaudio.data.CuratedStationsService
import com.easeaudio.data.RadioBrowserService
import com.easeaudio.data.iTunesPodcastService
import com.easeaudio.data.TrackArtworkService
import com.easeaudio.data.LyricsService
import com.easeaudio.data.SongLyrics
import com.easeaudio.firebase.FirebaseConfigManager
import com.easeaudio.network.NetworkQualityManager
import com.easeaudio.network.NetworkStatus
import android.os.Build
import kotlin.random.Random
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackErrorDetails(
    val stationName: String,
    val stationId: String,
    val streamUrl: String,
    val resolvedUrl: String?,
    val errorCode: Int,
    val errorCodeName: String,
    val errorMessage: String?,
    val causeMessage: String?,
    val httpStatusCode: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFormattedLog(): String {
        return """
            |========== MEDIA3 STREAM PLAYBACK FAILURE ==========
            | Timestamp   : ${java.util.Date(timestamp)}
            | Station Name: $stationName
            | Station ID  : $stationId
            | Original URL: $streamUrl
            | Resolved URL: ${resolvedUrl ?: "N/A"}
            | Error Code  : $errorCode ($errorCodeName)
            | Error Msg   : ${errorMessage ?: "N/A"}
            | Cause Msg   : ${causeMessage ?: "N/A"}
            | HTTP Status : ${httpStatusCode ?: "N/A"}
            |====================================================
        """.trimMargin()
    }

    fun toUserSummary(): String {
        val codeStr = if (errorCodeName.isNotBlank()) errorCodeName else "Code $errorCode"
        val httpStr = if (httpStatusCode != null) " [HTTP $httpStatusCode]" else ""
        return "Failed to load $stationName ($codeStr$httpStr)"
    }
}

class RadioPlayerManager(private val context: Context) {

    companion object {
        @Volatile
        var sharedMediaLibrarySession: MediaLibrarySession? = null

        var sharedMediaSession: MediaSession?
            get() = sharedMediaLibrarySession
            set(value) {
                if (value is MediaLibrarySession) {
                    sharedMediaLibrarySession = value
                }
            }

        @Volatile
        private var instance: RadioPlayerManager? = null

        fun getInstance(context: Context): RadioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: RadioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val TAG = "RadioPlayerManager"
    // BUG-3 fix: scope is @Volatile var so release() can cancel it and a potential
    // re-initialization path can see the updated reference. SupervisorJob ensures one
    // failing child coroutine (e.g. a crashed wave loop) does not cancel siblings.
    @Volatile private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val networkQualityManager = NetworkQualityManager(context)
    val firebaseConfigManager = FirebaseConfigManager(context)

    val networkStatus: StateFlow<NetworkStatus> = networkQualityManager.networkStatus
    val remoteConfig = firebaseConfigManager.configState

    private var exoPlayer: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private var mediaSession: MediaSession?
        get() = mediaLibrarySession
        set(value) {
            if (value is MediaLibrarySession) {
                mediaLibrarySession = value
            }
        }
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    private val _playbackErrorDetails = MutableStateFlow<PlaybackErrorDetails?>(null)
    val playbackErrorDetails: StateFlow<PlaybackErrorDetails?> = _playbackErrorDetails.asStateFlow()

    private val _failedStationIds = MutableStateFlow<Set<String>>(emptySet())
    val failedStationIds: StateFlow<Set<String>> = _failedStationIds.asStateFlow()

    private var currentStationList: List<RadioStation> = emptyList()

    private val stationFailureCounts = mutableMapOf<String, Int>()
    private val nextCheckTime = mutableMapOf<String, Long>()
    private val knownStations = mutableMapOf<String, RadioStation>()
    private var silentCheckJob: Job? = null

    fun updateStationList(list: List<RadioStation>) {
        currentStationList = list
        list.forEach { station ->
            knownStations[station.id] = station
        }
    }

    private val _streamTitle = MutableStateFlow<String?>("Live Audio Stream")
    val streamTitle: StateFlow<String?> = _streamTitle.asStateFlow()

    private val _trackArtworkUrl = MutableStateFlow<String?>(null)
    val trackArtworkUrl: StateFlow<String?> = _trackArtworkUrl.asStateFlow()
    private var artworkFetchJob: Job? = null

    private val _currentLyrics = MutableStateFlow<SongLyrics?>(null)
    val currentLyrics: StateFlow<SongLyrics?> = _currentLyrics.asStateFlow()

    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()

    fun fetchLyricsForCurrentTrack() {
        val title = _streamTitle.value ?: return
        val current = _currentStation.value
        scope.launch {
            _isLoadingLyrics.value = true
            try {
                val lyrics = LyricsService.fetchLyrics(title, current?.name ?: "")
                _currentLyrics.value = lyrics
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching lyrics: ${e.message}")
            } finally {
                _isLoadingLyrics.value = false
            }
        }
    }

    // Visualizer simulated wave amplitudes (8 bars)
    private val _waveAmplitudes = MutableStateFlow(List(8) { 0.2f })
    val waveAmplitudes: StateFlow<List<Float>> = _waveAmplitudes.asStateFlow()

    // Volume level 0.0f to 1.0f
    private val _volume = MutableStateFlow(0.85f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Sleep Timer
    private val _sleepTimerMinutesRemaining = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesRemaining: StateFlow<Int?> = _sleepTimerMinutesRemaining.asStateFlow()

    // Podcast playback position (ms) and duration (ms)
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(0L)
    val totalDuration: StateFlow<Long> = _totalDuration.asStateFlow()

    // Playback Speed (1.0x, 1.25x, 1.5x, 2.0x)
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.playbackParameters = androidx.media3.common.PlaybackParameters(speed)
    }

    fun skipBackward(ms: Long = 15000L) {
        exoPlayer?.let { player ->
            val current = player.currentPosition
            val newPos = (current - ms).coerceAtLeast(0L)
            player.seekTo(newPos)
            _currentPosition.value = newPos
        }
    }

    fun skipForward(ms: Long = 30000L) {
        exoPlayer?.let { player ->
            val current = player.currentPosition
            val duration = player.duration.coerceAtLeast(0L)
            val newPos = if (duration > 0) (current + ms).coerceAtMost(duration) else current + ms
            player.seekTo(newPos)
            _currentPosition.value = newPos
        }
    }

    private var sleepTimerJob: Job? = null
    private var waveAnimationJob: Job? = null
    private var positionPollingJob: Job? = null

    init {
        // Pre-seed known stations from curated list immediately so MediaBrowser is never empty
        CuratedStationsService.defaultCuratedStations.forEach { st ->
            knownStations[st.id] = st
        }
        setupPlayer()
        observeNetworkChanges()
        startSilentChecking()
        // Async background pre-warm of stations from database and curated service
        scope.launch(Dispatchers.IO) {
            try {
                val dbStations = RadioDatabase.getDatabase(context).radioDao().getAllStationsDirect()
                dbStations.forEach { st -> knownStations[st.id] = st }
                if (currentStationList.isEmpty()) {
                    val list = if (dbStations.isNotEmpty()) dbStations else CuratedStationsService.defaultCuratedStations
                    currentStationList = list
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background pre-warm error: ${e.message}")
            }
        }
    }

    // BUG-4 fix: both flags are read/written from multiple threads (Main listener + IO coroutine);
    // @Volatile ensures visibility across threads without heavier synchronization.
    @Volatile private var isFallbackAttempt = false

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
        if (wifiManager != null) {
            @Suppress("DEPRECATION")
            val wifiLockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.net.wifi.WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager.createWifiLock(
                wifiLockMode,
                "EaseAudio:WifiLock"
            ).apply {
                setReferenceCounted(false)
            }
        }

        val netStatus = networkStatus.value
        val cfg = remoteConfig.value

        val minBuf = if (netStatus.isWifi) 8000 else cfg.minBufferMsCellular.toInt()
        val maxBuf = if (netStatus.isWifi) 25000 else cfg.maxBufferMsCellular.toInt()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBuf,
                maxBuf,
                2000,
                4000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val mediaSourceFactory = DefaultMediaSourceFactory(context.applicationContext)
            .setDataSourceFactory(httpDataSourceFactory)

        exoPlayer = ExoPlayer.Builder(context.applicationContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                volume = _volume.value
                addListener(object : Player.Listener {
                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                            initEqualizer(audioSessionId)
                            initVisualizer(audioSessionId)
                            initLoudnessEnhancer(audioSessionId)
                        }
                    }

                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _isPlaying.value = isPlayingNow
                        _isLoading.value = false
                        if (isPlayingNow) {
                            FirebaseManager.stopStreamBufferingTrace(success = true)
                            // Bug #3/#5: Do NOT call startForegroundService() here.
                            // MediaSessionService owns the foreground notification lifecycle.
                            // Calling startService() from inside the manager bypasses that,
                            // creating a foreground-service-without-notification window (ANR risk).
                            acquireLocks()
                            startWaveAnimation()
                            startPositionPolling()
                            _currentStation.value?.let { current ->
                                if (_failedStationIds.value.contains(current.id)) {
                                    _failedStationIds.value = _failedStationIds.value - current.id
                                }
                            }
                        } else {
                            releaseLocks()
                            stopWaveAnimation()
                            stopPositionPolling()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                _isLoading.value = true
                                _playbackError.value = null
                                _currentStation.value?.let { station ->
                                    FirebaseManager.startStreamBufferingTrace(station.name)
                                }
                            }
                            Player.STATE_READY -> {
                                _isLoading.value = false
                                _playbackError.value = null
                                FirebaseManager.stopStreamBufferingTrace(success = true)
                                _currentStation.value?.let { current ->
                                    if (_failedStationIds.value.contains(current.id)) {
                                        _failedStationIds.value = _failedStationIds.value - current.id
                                    }
                                }
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                                _isLoading.value = false
                            }
                            Player.STATE_IDLE -> {
                                _isLoading.value = false
                            }
                        }
                    }

                    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                        val title = mediaMetadata.title?.toString()
                        val artist = mediaMetadata.artist?.toString()
                        val albumTitle = mediaMetadata.albumTitle?.toString()
                        val genre = mediaMetadata.genre?.toString()

                        Log.d(TAG, "MediaMetadata updated -> Station: ${_currentStation.value?.name}, Title: $title, Artist: $artist, Album: $albumTitle, Genre: $genre")

                        val newStreamTitle = when {
                            !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$artist - $title"
                            !title.isNullOrBlank() -> title
                            !artist.isNullOrBlank() -> artist
                            else -> _currentStation.value?.name ?: context.getString(R.string.live_audio_stream)
                        }
                        _streamTitle.value = newStreamTitle
                        _currentLyrics.value = null

                        // Cancel previous artwork resolution and clear stale artwork immediately
                        artworkFetchJob?.cancel()
                        _trackArtworkUrl.value = null

                        // Asynchronously fetch live track album artwork from iTunes Search API
                        val current = _currentStation.value
                        val liveStreamDefault = context.getString(R.string.live_audio_stream)
                        if (current != null && !current.isPodcast &&
                            !newStreamTitle.equals(current.name, ignoreCase = true) &&
                            !newStreamTitle.equals(liveStreamDefault, ignoreCase = true)
                        ) {
                            artworkFetchJob = scope.launch {
                                val art = TrackArtworkService.fetchTrackArtwork(newStreamTitle, current.name)
                                if (isActive) {
                                    _trackArtworkUrl.value = art
                                }
                            }
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        for (group in tracks.groups) {
                            if (group.type == C.TRACK_TYPE_AUDIO) {
                                for (i in 0 until group.length) {
                                    if (group.isTrackSelected(i)) {
                                        val format = group.getTrackFormat(i)
                                        Log.d(TAG, "Audio Stream Track Active -> Format: ${format.sampleMimeType}, SampleRate: ${format.sampleRate}Hz, Channels: ${format.channelCount}, Bitrate: ${format.bitrate}")
                                    }
                                }
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        FirebaseManager.stopStreamBufferingTrace(success = false, reason = error.message ?: "Player Error")
                        var httpCode: Int? = null
                        var currentCause: Throwable? = error.cause
                        while (currentCause != null) {
                            if (currentCause is HttpDataSource.InvalidResponseCodeException) {
                                httpCode = currentCause.responseCode
                                break
                            }
                            currentCause = currentCause.cause
                        }

                        val current = _currentStation.value
                        val currentUrl = exoPlayer?.currentMediaItem?.localConfiguration?.uri?.toString() ?: current?.streamUrl

                        val details = PlaybackErrorDetails(
                            stationName = current?.name ?: "Unknown Station",
                            stationId = current?.id ?: "",
                            streamUrl = current?.streamUrl ?: "",
                            resolvedUrl = currentUrl,
                            errorCode = error.errorCode,
                            errorCodeName = error.errorCodeName,
                            errorMessage = error.message,
                            causeMessage = error.cause?.message,
                            httpStatusCode = httpCode
                        )

                        Log.e(TAG, details.toFormattedLog(), error)

                        try {
                            val crashlytics = com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                            crashlytics.setCustomKey("station_name", current?.name ?: "Unknown")
                            crashlytics.setCustomKey("station_id", current?.id ?: "")
                            crashlytics.setCustomKey("stream_url", current?.streamUrl ?: "")
                            crashlytics.setCustomKey("error_code_name", error.errorCodeName)
                            crashlytics.setCustomKey("error_message", error.message ?: "")
                            crashlytics.setCustomKey("http_status_code", httpCode ?: -1)
                            crashlytics.recordException(error)
                        } catch (e: Exception) {
                            // Firebase Crashlytics not configured or initialized yet
                        }

                        if (current != null && !isFallbackAttempt) {
                            val fallbackUrl = when {
                                currentUrl != null && currentUrl.startsWith("https://", ignoreCase = true) -> currentUrl.replaceFirst("https://", "http://", ignoreCase = true)
                                currentUrl != null && currentUrl.startsWith("http://", ignoreCase = true) -> currentUrl.replaceFirst("http://", "https://", ignoreCase = true)
                                else -> null
                            }
                            if (fallbackUrl != null && fallbackUrl != currentUrl) {
                                Log.w(TAG, "Stream failed with ${error.errorCodeName}. Retrying station '${current.name}' with fallback URL: $fallbackUrl")
                                playStationWithUrl(current, fallbackUrl, isFallback = true)
                                return
                            }
                        }

                        if (current != null && !isHealingAttempt) {
                            tryAutoHealStream(current, currentUrl)
                        }

                        _playbackErrorDetails.value = details
                        _isLoading.value = false
                        _isPlaying.value = false

                        try {
                            exoPlayer?.stop()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error stopping player on failure: ${e.message}")
                        }

                        val isConnected = networkStatus.value.isConnected
                        if (isConnected) {
                            _currentStation.value?.let { st ->
                                _failedStationIds.value = _failedStationIds.value + st.id
                            }
                            _playbackError.value = if (httpCode == 403 || error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                                context.getString(R.string.stream_offline_notice)
                            } else {
                                "${context.getString(R.string.unable_connect_error)} [${error.errorCodeName}]"
                            }
                        } else {
                            _playbackError.value = context.getString(R.string.no_internet_error)
                        }
                        stopWaveAnimation()
                    }
                })
            }

        exoPlayer?.let { player ->
            try {
                val intent = Intent(context, com.easeaudio.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val forwardingPlayer = object : androidx.media3.common.ForwardingPlayer(player) {
                    override fun getAvailableCommands(): Player.Commands {
                        return super.getAvailableCommands().buildUpon()
                            .add(Player.COMMAND_SEEK_TO_NEXT)
                            .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                            .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                            .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                            .remove(Player.COMMAND_SEEK_BACK)
                            .remove(Player.COMMAND_SEEK_FORWARD)
                            .build()
                    }

                    override fun isCommandAvailable(command: Int): Boolean {
                        if (command == Player.COMMAND_SEEK_TO_NEXT ||
                            command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                            command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                            command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                            return true
                        }
                        val currentStation = _currentStation.value
                        val isPodcast = currentStation?.isPodcast == true
                        return if (!isPodcast && (command == Player.COMMAND_SEEK_BACK || command == Player.COMMAND_SEEK_FORWARD)) {
                            false
                        } else {
                            super.isCommandAvailable(command)
                        }
                    }

                    override fun seekToNext() {
                        playNextStation(currentStationList)
                    }

                    override fun seekToNextMediaItem() {
                        playNextStation(currentStationList)
                    }

                    override fun seekToPrevious() {
                        playPreviousStation(currentStationList)
                    }

                    override fun seekToPreviousMediaItem() {
                        playPreviousStation(currentStationList)
                    }

                    override fun isCurrentMediaItemLive(): Boolean {
                        val currentStation = _currentStation.value
                        return currentStation?.isPodcast != true
                    }

                    override fun isCurrentMediaItemSeekable(): Boolean {
                        val currentStation = _currentStation.value
                        return currentStation?.isPodcast == true
                    }

                    override fun getPlayerError(): PlaybackException? {
                        // Prevent AAOS Car Media Center from entering FATAL_ERROR (blank black screen) on offline network drops
                        return null
                    }

                    override fun getPlaybackState(): Int {
                        val state = super.getPlaybackState()
                        return if (state == Player.STATE_IDLE && _currentStation.value != null) {
                            if (_isLoading.value) Player.STATE_BUFFERING else Player.STATE_READY
                        } else {
                            state
                        }
                    }

                    override fun getCurrentMediaItem(): MediaItem? {
                        val superItem = super.getCurrentMediaItem()
                        if (superItem != null) return superItem
                        val st = _currentStation.value
                        return if (st != null) stationToMediaItem(st) else null
                    }

                    override fun play() {
                        val currentStation = _currentStation.value
                        if (currentStation != null && (player.playbackState == Player.STATE_IDLE ||
                                    player.playbackState == Player.STATE_ENDED ||
                                    player.mediaItemCount == 0 ||
                                    player.currentMediaItem == null ||
                                    _playbackError.value != null)) {
                            playStation(currentStation)
                        } else {
                            super.play()
                        }
                    }
                }

                val mediaLibraryCallback = object : MediaLibrarySession.Callback {
                    override fun onGetLibraryRoot(
                        session: MediaLibrarySession,
                        browser: MediaSession.ControllerInfo,
                        params: MediaLibraryService.LibraryParams?
                    ): ListenableFuture<LibraryResult<MediaItem>> {
                        val rootItem = createFolderItem(
                            id = "root_neotune",
                            title = "Neotune",
                            mediaType = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                            subtitle = "Internet Radio & Podcasts"
                        )
                        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
                    }

                    override fun onGetChildren(
                        session: MediaLibrarySession,
                        browser: MediaSession.ControllerInfo,
                        parentId: String,
                        page: Int,
                        pageSize: Int,
                        params: MediaLibraryService.LibraryParams?
                    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val items = mutableListOf<MediaItem>()
                                when (parentId) {
                                    "root_neotune", "root", "/" -> {
                                        items.add(createFolderItem("folder_favorites", "Favorites", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS, "Starred stations & podcasts"))
                                        items.add(createFolderItem("folder_top", "Top & Trending", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS, "Featured global streams"))
                                        items.add(createFolderItem("folder_recent", "Recently Played", MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS, "Your recent stations"))
                                        items.add(createFolderItem("folder_genres", "Genres & Categories", MediaMetadata.MEDIA_TYPE_FOLDER_GENRES, "Browse by musical genre"))
                                        items.add(createFolderItem("folder_podcasts", "Podcasts", MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS, "Popular podcast shows"))
                                    }
                                    "folder_favorites" -> {
                                        val favs = RadioDatabase.getDatabase(context).favoriteDao().getAllFavoritesDirect()
                                        if (favs.isNotEmpty()) {
                                            items.addAll(favs.map { fav ->
                                                val st = knownStations[fav.id] ?: RadioStation(
                                                    id = fav.id,
                                                    name = fav.name,
                                                    streamUrl = fav.streamUrl,
                                                    genre = fav.genre,
                                                    country = fav.country,
                                                    imageUrl = fav.imageUrl,
                                                    bitrate = fav.bitrate,
                                                    codec = fav.codec,
                                                    isCustom = fav.isCustom,
                                                    isFavorite = true
                                                )
                                                knownStations[st.id] = st
                                                stationToMediaItem(st)
                                            })
                                        } else {
                                            val favList = knownStations.values.filter { it.isFavorite }
                                            if (favList.isNotEmpty()) {
                                                items.addAll(favList.map { stationToMediaItem(it) })
                                            } else {
                                                // If user has no starred favorites yet, show top curated stations
                                                items.addAll(CuratedStationsService.defaultCuratedStations.take(10).map { stationToMediaItem(it) })
                                            }
                                        }
                                    }
                                    "folder_top" -> {
                                        val topList = when {
                                            currentStationList.isNotEmpty() -> currentStationList
                                            else -> {
                                                val dbStations = RadioDatabase.getDatabase(context).radioDao().getAllStationsDirect()
                                                val list = if (dbStations.isNotEmpty()) dbStations else CuratedStationsService.defaultCuratedStations
                                                currentStationList = list
                                                list.forEach { knownStations[it.id] = it }
                                                list
                                            }
                                        }
                                        items.addAll(topList.take(40).map { stationToMediaItem(it) })
                                    }
                                    "folder_recent" -> {
                                        val recents = RadioDatabase.getDatabase(context).radioDao().getRecentStationsDirect()
                                        if (recents.isNotEmpty()) {
                                            recents.forEach { knownStations[it.id] = it }
                                            items.addAll(recents.map { stationToMediaItem(it) })
                                        } else {
                                            items.addAll(CuratedStationsService.defaultCuratedStations.take(10).map { stationToMediaItem(it) })
                                        }
                                    }
                                    "folder_genres" -> {
                                        val genreList = listOf(
                                            "Pop" to "genre_pop",
                                            "Rock" to "genre_rock",
                                            "Jazz" to "genre_jazz",
                                            "Classical" to "genre_classical",
                                            "News & Talk" to "genre_news",
                                            "Electronic" to "genre_electronic",
                                            "Country" to "genre_country",
                                            "Dance" to "genre_dance",
                                            "Ambient" to "genre_ambient",
                                            "Hip Hop" to "genre_hiphop",
                                            "Lo-Fi" to "genre_lofi",
                                            "Metal" to "genre_metal",
                                            "Blues" to "genre_blues"
                                        )
                                        items.addAll(genreList.map { (name, id) ->
                                            createFolderItem(id, name, MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                                        })
                                    }
                                    "folder_podcasts" -> {
                                        val podcasts = knownStations.values.filter { it.isPodcast }.ifEmpty {
                                            CuratedStationsService.defaultCuratedStations.filter { it.isPodcast }
                                        }
                                        items.addAll(podcasts.map { stationToMediaItem(it) })
                                    }
                                    else -> {
                                        if (parentId.startsWith("genre_")) {
                                            val genreKeyword = parentId.removePrefix("genre_")
                                            val matching = knownStations.values.filter {
                                                it.genre.contains(genreKeyword, ignoreCase = true) ||
                                                it.name.contains(genreKeyword, ignoreCase = true)
                                            }.toMutableList()

                                            if (matching.isEmpty()) {
                                                val curatedMatches = CuratedStationsService.defaultCuratedStations.filter {
                                                    it.genre.contains(genreKeyword, ignoreCase = true) ||
                                                    it.name.contains(genreKeyword, ignoreCase = true)
                                                }
                                                matching.addAll(curatedMatches)
                                            }
                                            if (matching.isEmpty()) {
                                                matching.addAll(CuratedStationsService.defaultCuratedStations.take(10))
                                            }
                                            items.addAll(matching.distinctBy { it.id }.take(30).map { stationToMediaItem(it) })
                                        }
                                    }
                                }

                                val fromIndex = (page * pageSize).coerceAtMost(items.size)
                                val toIndex = if (pageSize > 0) ((page + 1) * pageSize).coerceAtMost(items.size) else items.size
                                val paged = if (fromIndex < toIndex) items.subList(fromIndex, toIndex) else items
                                future.set(LibraryResult.ofItemList(ImmutableList.copyOf(paged), params))
                            } catch (e: Exception) {
                                Log.e(TAG, "Error resolving children for $parentId: ${e.message}", e)
                                future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                            }
                        }
                        return future
                    }

                    override fun onGetItem(
                        session: MediaLibrarySession,
                        browser: MediaSession.ControllerInfo,
                        mediaId: String
                    ): ListenableFuture<LibraryResult<MediaItem>> {
                        val station = knownStations[mediaId]
                        if (station != null) {
                            return Futures.immediateFuture(LibraryResult.ofItem(stationToMediaItem(station), null))
                        }
                        if (mediaId.startsWith("folder_") || mediaId.startsWith("genre_") || mediaId == "root_neotune") {
                            val title = mediaId.removePrefix("folder_").removePrefix("genre_").replaceFirstChar { it.uppercase() }
                            return Futures.immediateFuture(LibraryResult.ofItem(createFolderItem(mediaId, title, MediaMetadata.MEDIA_TYPE_FOLDER_MIXED), null))
                        }
                        val future = SettableFuture.create<LibraryResult<MediaItem>>()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val dbStation = RadioDatabase.getDatabase(context).radioDao().getStationById(mediaId)
                                if (dbStation != null) {
                                    knownStations[dbStation.id] = dbStation
                                    future.set(LibraryResult.ofItem(stationToMediaItem(dbStation), null))
                                } else {
                                    future.set(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE))
                                }
                            } catch (e: Exception) {
                                future.set(LibraryResult.ofError(SessionError.ERROR_UNKNOWN))
                            }
                        }
                        return future
                    }

                    override fun onAddMediaItems(
                        mediaSession: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        mediaItems: MutableList<MediaItem>
                    ): ListenableFuture<MutableList<MediaItem>> {
                        val updatedItems = mediaItems.map { item ->
                            val station = knownStations[item.mediaId]
                            if (station != null) {
                                stationToMediaItem(station)
                            } else {
                                item
                            }
                        }.toMutableList()

                        val targetItem = updatedItems.firstOrNull()
                        if (targetItem != null) {
                            val targetId = targetItem.mediaId
                            val streamUriStr = targetItem.localConfiguration?.uri?.toString()
                                ?: targetItem.requestMetadata.mediaUri?.toString()
                                ?: targetId
                            val st = knownStations[targetId] ?: RadioStation(
                                id = targetId,
                                name = targetItem.mediaMetadata.title?.toString() ?: "Station",
                                streamUrl = streamUriStr,
                                genre = targetItem.mediaMetadata.artist?.toString() ?: "Radio",
                                country = "Global",
                                imageUrl = targetItem.mediaMetadata.artworkUri?.toString() ?: ""
                            )
                            scope.launch(Dispatchers.Main) {
                                playStation(st)
                            }
                        }
                        return Futures.immediateFuture(updatedItems)
                    }

                    override fun onSearch(
                        session: MediaLibrarySession,
                        browser: MediaSession.ControllerInfo,
                        query: String,
                        params: MediaLibraryService.LibraryParams?
                    ): ListenableFuture<LibraryResult<Void>> {
                        val future = SettableFuture.create<LibraryResult<Void>>()
                        scope.launch(Dispatchers.IO) {
                            val matching = searchStationsAsync(query)
                            session.notifySearchResultChanged(browser, query, matching.size, params)
                            future.set(LibraryResult.ofVoid(params))
                        }
                        return future
                    }

                    override fun onGetSearchResult(
                        session: MediaLibrarySession,
                        browser: MediaSession.ControllerInfo,
                        query: String,
                        page: Int,
                        pageSize: Int,
                        params: MediaLibraryService.LibraryParams?
                    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                        val future = SettableFuture.create<LibraryResult<ImmutableList<MediaItem>>>()
                        scope.launch(Dispatchers.IO) {
                            val matching = searchStationsAsync(query)
                            val fromIndex = (page * pageSize).coerceAtMost(matching.size)
                            val toIndex = if (pageSize > 0) ((page + 1) * pageSize).coerceAtMost(matching.size) else matching.size
                            val subList = if (fromIndex < toIndex) matching.subList(fromIndex, toIndex) else matching
                            val mediaItems = subList.map { stationToMediaItem(it) }
                            future.set(LibraryResult.ofItemList(ImmutableList.copyOf(mediaItems), params))
                        }
                        return future
                    }

                    override fun onPlaybackResumption(
                        mediaSession: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                        val current = _currentStation.value
                        if (current != null) {
                            val item = stationToMediaItem(current)
                            val itemsWithStart = MediaSession.MediaItemsWithStartPosition(
                                ImmutableList.of(item),
                                0,
                                exoPlayer?.currentPosition ?: 0L
                            )
                            return Futures.immediateFuture(itemsWithStart)
                        }
                        val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                        scope.launch(Dispatchers.IO) {
                            try {
                                val recents = RadioDatabase.getDatabase(context).radioDao().getRecentStationsDirect()
                                val candidate = recents.firstOrNull() ?: currentStationList.firstOrNull()
                                if (candidate != null) {
                                    knownStations[candidate.id] = candidate
                                    val item = stationToMediaItem(candidate)
                                    val itemsWithStart = MediaSession.MediaItemsWithStartPosition(
                                        ImmutableList.of(item),
                                        0,
                                        0L
                                    )
                                    withContext(Dispatchers.Main) {
                                        setPreloadedStation(candidate)
                                    }
                                    future.set(itemsWithStart)
                                } else {
                                    future.setException(UnsupportedOperationException("No station to resume"))
                                }
                            } catch (e: Exception) {
                                future.setException(e)
                            }
                        }
                        return future
                    }

                    @Suppress("DEPRECATION")
                    override fun onPlayerCommandRequest(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        playerCommand: Int
                    ): Int {
                        if (playerCommand == Player.COMMAND_SEEK_TO_NEXT) {
                            scope.launch(Dispatchers.Main) {
                                playNextStation(currentStationList)
                            }
                        }
                        if (playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS) {
                            scope.launch(Dispatchers.Main) {
                                playPreviousStation(currentStationList)
                            }
                        }
                        return super.onPlayerCommandRequest(session, controller, playerCommand)
                    }
                }

                mediaLibrarySession = MediaLibrarySession.Builder(context.applicationContext, forwardingPlayer, mediaLibraryCallback)
                    .setSessionActivity(pendingIntent)
                    .build().also {
                        sharedMediaLibrarySession = it
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create MediaLibrarySession: ${e.message}", e)
            }
        }
    }

    fun stationToMediaItem(station: RadioStation): MediaItem {
        val artworkUri = try {
            if (station.imageUrl.isNotBlank()) Uri.parse(station.imageUrl) else null
        } catch (e: Exception) {
            null
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(station.name)
            .setArtist(station.genre)
            .setSubtitle(if (station.isPodcast) "Podcast" else station.country)
            .setArtworkUri(artworkUri)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(if (station.isPodcast) MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE else MediaMetadata.MEDIA_TYPE_RADIO_STATION)
            .build()

        return MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun createFolderItem(id: String, title: String, mediaType: Int, subtitle: String? = null): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setIsPlayable(false)
            .setIsBrowsable(true)
            .setMediaType(mediaType)
            .build()

        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }

    private suspend fun searchStationsAsync(query: String): List<RadioStation> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isBlank()) return@withContext (if (currentStationList.isNotEmpty()) currentStationList else CuratedStationsService.defaultCuratedStations).take(20)
        val localMatches = knownStations.values.filter {
            it.name.lowercase().contains(q) ||
            it.genre.lowercase().contains(q) ||
            it.country.lowercase().contains(q)
        }.toMutableList()

        if (localMatches.size < 10) {
            try {
                val onlineMatches = com.easeaudio.data.RadioBrowserService.fetchTopStations(limit = 30, searchQuery = q)
                onlineMatches.forEach { knownStations[it.id] = it }
                localMatches.addAll(onlineMatches)
            } catch (e: Exception) {
                Log.w(TAG, "Online search error for '$query': ${e.message}")
            }
        }
        return@withContext localMatches.distinctBy { it.id }.take(30)
    }

    @Volatile private var isHealingAttempt = false

    private fun tryAutoHealStream(station: RadioStation, failedUrl: String?) {
        if (station.isPodcast || isHealingAttempt) return
        isHealingAttempt = true
        scope.launch(Dispatchers.IO) {
            try {
                Log.i(TAG, "Attempting automated online stream healing for station '${station.name}'...")
                val queryName = station.name.split("-").firstOrNull()?.trim() ?: station.name
                val results = com.easeaudio.data.RadioBrowserService.fetchTopStations(
                    limit = 10,
                    searchQuery = queryName,
                    country = if (station.country != "Global") station.country else ""
                )

                val freshStream = results.firstOrNull { candidate ->
                    candidate.streamUrl.isNotBlank() &&
                            candidate.streamUrl != station.streamUrl &&
                            candidate.streamUrl != failedUrl
                }

                if (freshStream != null) {
                    Log.i(TAG, "Stream Auto-Healed! Switched '${station.name}' to fresh mirror: ${freshStream.streamUrl}")
                    withContext(Dispatchers.Main) {
                        val updatedStation = station.copy(streamUrl = freshStream.streamUrl)
                        _currentStation.value = updatedStation
                        playStationWithUrl(updatedStation, freshStream.streamUrl, isFallback = true)
                    }
                    return@launch
                }
            } catch (e: Exception) {
                Log.e(TAG, "Auto-healing failed for ${station.name}: ${e.message}")
            } finally {
                isHealingAttempt = false
            }
        }
    }


    private fun observeNetworkChanges() {
        scope.launch {
            networkStatus.collect { status ->
                Log.d(TAG, "Network condition updated: ${status.label}")
                // If currently playing and network recovers or changes, ensure continuity
                if (_isPlaying.value && !status.isConnected) {
                    _playbackError.value = context.getString(R.string.waiting_network_reconnect)
                } else if (_isPlaying.value && status.isConnected && _playbackError.value != null) {
                    _playbackError.value = null
                }
            }
        }
    }

    private suspend fun resolveDirectStreamUrl(rawUrl: String): String = withContext(Dispatchers.IO) {
        if (rawUrl.isBlank()) return@withContext rawUrl
        var currentUrl = rawUrl.trim()
        
        // Rewrite old/broken VOV (Voice of Vietnam) stream URLs on audio-lss.vov.vn to their working, live formats
        if (currentUrl.contains("audio-lss.vov.vn", ignoreCase = true)) {
            val vovMatch = Regex("""\b(vov\d+)\b""", RegexOption.IGNORE_CASE).find(currentUrl)
            if (vovMatch != null) {
                val channel = vovMatch.groupValues[1].lowercase()
                currentUrl = "https://audio-lss.vov.vn/live/$channel.m3u8"
                Log.d(TAG, "Rewrote old VOV stream URL to active live URL: $currentUrl")
            }
        }

        try {
            var redirectCount = 0
            while (redirectCount < 3) {
                val lower = currentUrl.lowercase()
                val isPlaylist = lower.endsWith(".m3u") || lower.endsWith(".pls") || 
                                 lower.contains(".m3u?") || lower.contains(".pls?") || 
                                 lower.endsWith(".m3u8") || lower.contains(".m3u8?")
                val isDirectAudio = lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".ogg") || lower.endsWith(".flac") || lower.endsWith(".wav")

                val connection = (java.net.URL(currentUrl).openConnection() as java.net.HttpURLConnection).apply {
                    (this as? javax.net.ssl.HttpsURLConnection)?.apply {
                        sslSocketFactory = com.easeaudio.util.NetworkSecurityHelper.sslSocketFactory
                        hostnameVerifier = com.easeaudio.util.NetworkSecurityHelper.hostnameVerifier
                    }
                    connectTimeout = 8000
                    readTimeout = 8000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    setRequestProperty("Accept", "*/*")
                }
                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = location.trim()
                        redirectCount++
                        continue
                    }
                }

                if (responseCode in 200..299) {
                    val contentType = (connection.contentType ?: "").lowercase()
                    if (isDirectAudio || contentType.contains("audio") || contentType.contains("mpeg") || contentType.contains("ogg") || contentType.contains("aac") || contentType.contains("flac") || contentType.contains("wav")) {
                        connection.disconnect()
                        return@withContext currentUrl
                    }

                    // Safely read up to 256KB of text to resolve playlists/RSS XML without reading large binary audio streams into memory
                    val content = try {
                        val isGzip = connection.contentEncoding?.contains("gzip", ignoreCase = true) == true
                        val inputStream = if (isGzip) java.util.zip.GZIPInputStream(connection.inputStream) else connection.inputStream
                        inputStream.bufferedReader().use { reader ->
                            val charBuffer = CharArray(262144)
                            val readCount = reader.read(charBuffer, 0, charBuffer.size)
                            if (readCount > 0) String(charBuffer, 0, readCount) else ""
                        }
                    } catch (e: Exception) {
                        ""
                    } finally {
                        connection.disconnect()
                    }

                    if (content.isNotBlank()) {
                        // 1. Check for RSS / Podcast enclosure URL (latest episode MP3/AAC)
                        if (content.contains("<rss", ignoreCase = true) || content.contains("<enclosure", ignoreCase = true) || content.contains("<channel", ignoreCase = true)) {
                            val enclosureMatch = Regex("""<enclosure[^>]+url=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(content)
                                ?: Regex("""<media:content[^>]+url=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(content)
                                ?: Regex("""(https?://[^\s"'<>]+\.(?:mp3|m4a|aac|ogg)[^\s"'<>]*)""", RegexOption.IGNORE_CASE).find(content)
                            
                            if (enclosureMatch != null) {
                                val resolvedMediaUrl = enclosureMatch.groupValues[1].trim().replace("&amp;", "&")
                                Log.i(TAG, "Resolved podcast RSS feed $rawUrl to playable audio URL: $resolvedMediaUrl")
                                return@withContext resolvedMediaUrl
                            }
                        }

                        // 2. Check for M3U / PLS playlist
                        if (lower.contains(".pls") || content.contains("[playlist]", ignoreCase = true)) {
                            val match = Regex("""File\d+=(http[s]?://[^\s\r\n]+)""", RegexOption.IGNORE_CASE).find(content)
                            if (match != null) {
                                return@withContext match.groupValues[1].trim()
                            }
                        }
                        val firstHttpLine = content.lines().firstOrNull { line ->
                            val trimmed = line.trim()
                            !trimmed.startsWith("#") && (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true))
                        }
                        if (firstHttpLine != null) {
                            return@withContext firstHttpLine.trim()
                        }
                    }
                    return@withContext currentUrl
                }
                connection.disconnect()
                break
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve playlist URL $rawUrl: ${e.message}")
        }
        return@withContext currentUrl
    }

    private var playbackJob: Job? = null

    fun playStation(station: RadioStation) {
        playStationWithUrl(station, station.streamUrl, isFallback = false)
    }

    fun playPodcastEpisode(show: RadioStation, episode: com.easeaudio.data.PodcastEpisode) {
        _currentStation.value = show
        _streamTitle.value = episode.title
        playStationWithUrl(
            station = show.copy(
                imageUrl = episode.artworkUrl.ifBlank { show.imageUrl }
            ),
            targetUrl = episode.audioUrl,
            isFallback = false
        )
    }

    fun setPreloadedStation(station: RadioStation) {
        _currentStation.value = station
        _streamTitle.value = station.name
        _playbackError.value = null
        _isLoading.value = false
        _isPlaying.value = false
    }

    private fun playStationWithUrl(station: RadioStation, targetUrl: String, isFallback: Boolean) {
        // Cancel any pending URL resolution / player prep job to prevent race conditions when switching quickly
        playbackJob?.cancel()
        artworkFetchJob?.cancel()
        
        _currentStation.value = station
        _playbackError.value = null
        _isLoading.value = true
        _streamTitle.value = station.name
        _trackArtworkUrl.value = null
        _currentLyrics.value = null
        isFallbackAttempt = isFallback

        // Immediately stop previous playback so old podcast/stream stops playing instantly while resolving new URL
        exoPlayer?.let { player ->
            try {
                player.stop()
                player.clearMediaItems()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping previous player: ${e.message}")
            }
        }
        // Pre-populate player metadata immediately so Automotive Now Playing screen shows station info right away
        exoPlayer?.let { player ->
            try {
                val artworkUri = if (station.imageUrl.isNotBlank()) {
                    try { Uri.parse(station.imageUrl) } catch (_: Exception) { null }
                } else null

                val immediateMetadata = MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.genre)
                    .setSubtitle(if (station.isPodcast) "Podcast" else station.country)
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(true)
                    .build()

                val immediateItem = MediaItem.Builder()
                    .setMediaId(station.id)
                    .setUri(targetUrl)
                    .setMediaMetadata(immediateMetadata)
                    .build()

                player.setMediaItem(immediateItem)
            } catch (e: Exception) {
                Log.w(TAG, "Error setting initial media item: ${e.message}")
            }
        }

        try {
            val intent = Intent(context, RadioPlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RadioPlaybackService", e)
        }

        playbackJob = scope.launch {
            val resolvedUrl = resolveDirectStreamUrl(targetUrl)
            exoPlayer?.let { player ->
                val artworkUri = if (station.imageUrl.isNotBlank()) {
                    try { Uri.parse(station.imageUrl) } catch (_: Exception) { null }
                } else null

                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.genre)
                    .setSubtitle(if (station.isPodcast) "Podcast" else station.country)
                    .setArtworkUri(artworkUri)
                    .setIsPlayable(true)
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(station.id)
                    .setUri(resolvedUrl)
                    .setMediaMetadata(mediaMetadata)
                    .build()

                player.setMediaItem(mediaItem)
                player.prepare()
                if (station.isPodcast) {
                    val progress = com.easeaudio.data.PodcastProgressManager.getProgress(context, station.streamUrl.ifBlank { station.id })
                    if (progress != null && progress.positionMs > 3000L) {
                        player.seekTo(progress.positionMs)
                        _currentPosition.value = progress.positionMs
                        Log.i(TAG, "Resumed podcast '${station.name}' at position ${progress.positionMs / 1000}s")
                    }
                }
                player.play()
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                val current = _currentStation.value
                if (current != null && (player.playbackState == Player.STATE_IDLE ||
                            player.playbackState == Player.STATE_ENDED ||
                            player.mediaItemCount == 0 ||
                            player.currentMediaItem == null ||
                            _playbackError.value != null)
                ) {
                    playStation(current)
                } else if (current == null) {
                    val fallback = currentStationList.firstOrNull() ?: knownStations.values.firstOrNull()
                    fallback?.let { playStation(it) }
                } else {
                    try {
                        val intent = Intent(context, RadioPlaybackService::class.java)
                        context.startService(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start RadioPlaybackService on resume", e)
                    }
                    player.play()
                }
            }
        }
    }

    fun stopPlayer() {
        exoPlayer?.let { player ->
            try {
                player.stop()
                player.clearMediaItems()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping ExoPlayer: ${e.message}")
            }
        }
        _isPlaying.value = false
        _isLoading.value = false
    }

    fun retryCurrentStation() {
        _currentStation.value?.let { playStation(it) }
    }

    fun playNextStation(stationList: List<RadioStation> = emptyList()) {
        val list = when {
            stationList.isNotEmpty() -> stationList
            currentStationList.isNotEmpty() -> currentStationList
            knownStations.isNotEmpty() -> knownStations.values.toList()
            else -> CuratedStationsService.defaultCuratedStations
        }
        if (list.isEmpty()) return
        val current = _currentStation.value ?: return playStation(list.first())
        val currentIndex = list.indexOfFirst { it.id == current.id }
        val nextIndex = if (currentIndex >= 0 && currentIndex < list.size - 1) currentIndex + 1 else 0
        playStation(list[nextIndex])
    }

    fun playPreviousStation(stationList: List<RadioStation> = emptyList()) {
        val list = when {
            stationList.isNotEmpty() -> stationList
            currentStationList.isNotEmpty() -> currentStationList
            knownStations.isNotEmpty() -> knownStations.values.toList()
            else -> CuratedStationsService.defaultCuratedStations
        }
        if (list.isEmpty()) return
        val current = _currentStation.value ?: return playStation(list.last())
        val currentIndex = list.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        playStation(list[prevIndex])
    }

    fun seekRelative(offsetMs: Long) {
        exoPlayer?.let { player ->
            val targetPos = (player.currentPosition + offsetMs).coerceAtLeast(0L)
            player.seekTo(targetPos)
        }
    }

    fun setVolume(newVolume: Float) {
        _volume.value = newVolume.coerceIn(0.0f, 1.0f)
        exoPlayer?.volume = _volume.value
    }

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        _sleepTimerMinutesRemaining.value = minutes

        sleepTimerJob = scope.launch {
            var remainingSeconds = minutes * 60
            while (isActive && remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                _sleepTimerMinutesRemaining.value = (remainingSeconds + 59) / 60
            }
            // Timer expired -> fade out and stop playback.
            // Bug #7: Capture a local reference to avoid accessing a released exoPlayer
            // across suspension points (delay) if release() is called concurrently.
            val player = exoPlayer ?: return@launch
            for (i in 10 downTo 0) {
                player.volume = (_volume.value * (i / 10.0f))
                delay(150L)
            }
            player.pause()
            player.volume = _volume.value
            _sleepTimerMinutesRemaining.value = null
        }
    }

    private var equalizer: android.media.audiofx.Equalizer? = null
    private var currentEqPreset: String = "Balanced"

    fun setEqPreset(presetName: String) {
        currentEqPreset = presetName
        applyEqPreset(presetName)
    }

    private var audioVisualizer: android.media.audiofx.Visualizer? = null
    private var lastVisualizerUpdateTime: Long = 0L
    private var isBatterySaverMode = false

    fun setBatterySaverMode(enabled: Boolean) {
        isBatterySaverMode = enabled
        if (enabled) {
            Log.i(TAG, "Battery Saver Mode enabled: Throttling polling and background updates")
        }
    }

    fun refreshStreamMetadata() {
        try {
            exoPlayer?.mediaMetadata?.let { mediaMetadata ->
                val title = mediaMetadata.title?.toString()
                val artist = mediaMetadata.artist?.toString()
                if (!title.isNullOrBlank() || !artist.isNullOrBlank()) {
                    _streamTitle.value = when {
                        !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$artist - $title"
                        !title.isNullOrBlank() -> title
                        else -> artist ?: ""
                    }
                }
            }
            _currentStation.value?.let { station ->
                if (_streamTitle.value.isNullOrBlank() || _streamTitle.value == context.getString(R.string.live_audio_stream)) {
                    _streamTitle.value = station.name
                }
            }
            Log.d(TAG, "Refreshed current stream metadata: ${_streamTitle.value}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to refresh stream metadata: ${e.message}")
        }
    }

    fun updateMediaSessionMetadata(stationName: String, trackTitle: String, genre: String = "", artworkUri: Uri? = null) {
        try {
            val displayTitle = if (trackTitle.isNotBlank()) trackTitle else stationName
            val displayArtist = if (trackTitle.isNotBlank()) stationName else genre.ifBlank { "NeoTune Radio" }

            val metadata = MediaMetadata.Builder()
                .setTitle(displayTitle)
                .setArtist(displayArtist)
                .setAlbumTitle(stationName)
                .setDisplayTitle(displayTitle)
                .setSubtitle(displayArtist)
                .setGenre(genre)
                .setArtworkUri(artworkUri)
                .build()

            exoPlayer?.let { player ->
                val currentItem = player.currentMediaItem
                if (currentItem != null) {
                    val newItem = currentItem.buildUpon().setMediaMetadata(metadata).build()
                    player.replaceMediaItem(player.currentMediaItemIndex, newItem)
                }
            }
            Log.d(TAG, "Updated Bluetooth/MediaSession Metadata: $displayTitle ($displayArtist)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update MediaSession metadata: ${e.message}")
        }
    }

    @OptIn(UnstableApi::class)
    private fun initVisualizer(audioSessionId: Int) {
        try {
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
            audioVisualizer?.release()
            audioVisualizer = android.media.audiofx.Visualizer(audioSessionId).apply {
                val ranges = android.media.audiofx.Visualizer.getCaptureSizeRange()
                captureSize = 128.coerceIn(ranges[0], ranges[1])
                setDataCaptureListener(
                    object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: android.media.audiofx.Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform != null && _isPlaying.value) {
                                processWaveformData(waveform)
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: android.media.audiofx.Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null && _isPlaying.value) {
                                processFftData(fft)
                            }
                        }
                    },
                    android.media.audiofx.Visualizer.getMaxCaptureRate() / 2,
                    true,
                    true
                )
                enabled = true
            }
            Log.d(TAG, "Audio Visualizer initialized for audioSessionId=$audioSessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Audio Visualizer not supported or lacking permission: ${e.message}")
            audioVisualizer = null
        }
    }

    private fun processFftData(fft: ByteArray) {
        if (fft.isEmpty() || !_isPlaying.value) return
        val numBands = 8
        val numBins = (fft.size / 2).coerceAtLeast(1)
        val magnitudes = FloatArray(numBins)
        for (k in 1 until numBins) {
            val r = fft[2 * k].toFloat()
            val i = fft[2 * k + 1].toFloat()
            magnitudes[k] = kotlin.math.hypot(r, i)
        }

        val binsPerBand = ((numBins - 1) / numBands).coerceAtLeast(1)
        val volFactor = _volume.value.coerceIn(0.2f, 1.0f)
        val currentAmps = _waveAmplitudes.value

        val newAmplitudes = List(numBands) { band ->
            var sum = 0f
            val startBin = 1 + band * binsPerBand
            val endBin = (startBin + binsPerBand).coerceAtMost(numBins)
            var count = 0
            for (k in startBin until endBin) {
                sum += magnitudes[k]
                count++
            }
            val avgMag = if (count > 0) sum / count else 0f
            val target = ((avgMag / 48f) * volFactor).coerceIn(0.08f, 1.0f)
            val prev = currentAmps.getOrElse(band) { 0.15f }
            val smoothed = if (target > prev) prev + (target - prev) * 0.65f else prev + (target - prev) * 0.3f
            smoothed.coerceIn(0.08f, 1.0f)
        }

        _waveAmplitudes.value = newAmplitudes
        lastVisualizerUpdateTime = System.currentTimeMillis()
    }

    private fun processWaveformData(waveform: ByteArray) {
        if (waveform.isEmpty() || !_isPlaying.value) return
        val numBands = 8
        val chunkSize = (waveform.size / numBands).coerceAtLeast(1)
        val volFactor = _volume.value.coerceIn(0.2f, 1.0f)
        val currentAmps = _waveAmplitudes.value

        val newAmplitudes = List(numBands) { i ->
            var sum = 0f
            val start = i * chunkSize
            val end = (start + chunkSize).coerceAtMost(waveform.size)
            for (j in start until end) {
                val sample = waveform[j].toInt() and 0xFF
                sum += kotlin.math.abs(sample - 128)
            }
            val count = (end - start).coerceAtLeast(1)
            val avg = sum / count
            val target = ((avg / 64f) * volFactor).coerceIn(0.08f, 1.0f)
            val prev = currentAmps.getOrElse(i) { 0.15f }
            val smoothed = if (target > prev) prev + (target - prev) * 0.65f else prev + (target - prev) * 0.3f
            smoothed.coerceIn(0.08f, 1.0f)
        }

        _waveAmplitudes.value = newAmplitudes
        lastVisualizerUpdateTime = System.currentTimeMillis()
    }

    private var loudnessEnhancer: android.media.audiofx.LoudnessEnhancer? = null
    private val _isAudioBoosterEnabled = MutableStateFlow(true)
    val isAudioBoosterEnabled: StateFlow<Boolean> = _isAudioBoosterEnabled.asStateFlow()

    @OptIn(UnstableApi::class)
    private fun initLoudnessEnhancer(audioSessionId: Int) {
        try {
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
            loudnessEnhancer?.release()
            loudnessEnhancer = android.media.audiofx.LoudnessEnhancer(audioSessionId).apply {
                setTargetGain(600) // +6dB gain normalization for streams
                enabled = _isAudioBoosterEnabled.value
            }
            Log.d(TAG, "LoudnessEnhancer initialized for audioSessionId=$audioSessionId, enabled=${_isAudioBoosterEnabled.value}")
        } catch (e: Exception) {
            Log.w(TAG, "LoudnessEnhancer not supported: ${e.message}")
        }
    }

    fun setAudioBoosterEnabled(enabled: Boolean) {
        _isAudioBoosterEnabled.value = enabled
        try {
            loudnessEnhancer?.enabled = enabled
            Log.d(TAG, "Audio booster set to: $enabled")
        } catch (e: Exception) {
            Log.w(TAG, "Error toggling LoudnessEnhancer: ${e.message}")
        }
    }

    @OptIn(UnstableApi::class)
    private fun initEqualizer(audioSessionId: Int) {
        try {
            if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
            equalizer?.release()
            equalizer = android.media.audiofx.Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            applyEqPreset(currentEqPreset)
            Log.d(TAG, "Equalizer initialized for audioSessionId=$audioSessionId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Equalizer: ${e.message}")
        }
    }

    private fun applyEqPreset(presetName: String) {
        val eq = equalizer ?: return
        try {
            if (!eq.enabled) eq.enabled = true
            val numBands = eq.numberOfBands
            if (numBands == 0.toShort()) return
            val minLevel = eq.bandLevelRange[0]
            val maxLevel = eq.bandLevelRange[1]
            val midLevel = 0.toShort()

            for (i in 0 until numBands) {
                val freq = eq.getCenterFreq(i.toShort()) / 1000
                val targetDb: Short = when (presetName) {
                    "Bass Boost" -> if (freq < 300) (maxLevel * 0.7f).toInt().toShort() else if (freq > 3000) (minLevel * 0.2f).toInt().toShort() else midLevel
                    "Chill Lounge" -> if (freq < 250 || freq > 4000) (maxLevel * 0.4f).toInt().toShort() else (minLevel * 0.2f).toInt().toShort()
                    "Acoustic" -> if (freq in 1000..8000) (maxLevel * 0.5f).toInt().toShort() else midLevel
                    "Vocal Focus", "Speech" -> if (freq in 800..3500) (maxLevel * 0.6f).toInt().toShort() else if (freq < 200) (minLevel * 0.5f).toInt().toShort() else midLevel
                    else -> midLevel
                }
                eq.setBandLevel(i.toShort(), targetDb.coerceIn(minLevel, maxLevel))
            }
            Log.d(TAG, "Applied EQ preset $presetName across $numBands bands")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply EQ preset $presetName: ${e.message}")
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesRemaining.value = null
    }

    private fun startWaveAnimation() {
        waveAnimationJob?.cancel()
        waveAnimationJob = scope.launch {
            var time = 0.0f
            while (isActive && _isPlaying.value) {
                val now = System.currentTimeMillis()
                if (audioVisualizer != null && (now - lastVisualizerUpdateTime < 1000L)) {
                    // Actual visualizer is active and providing real data, wait
                    delay(500L)
                    continue
                }
                delay(if (isBatterySaverMode) 500L else 120L)
                time += 0.3f
                val volFactor = _volume.value.coerceIn(0.2f, 1.0f)
                val newAmplitudes = List(8) { index ->
                    // Combination of offset sine waves and slight noise creates organic, fluid audio-reactive behavior
                    val wave1 = kotlin.math.sin(time + index * 0.6f) * 0.35f
                    val wave2 = kotlin.math.sin(time * 0.75f - index * 1.1f) * 0.25f
                    val noise = (Random.nextFloat() - 0.5f) * 0.15f
                    val base = 0.45f + wave1 + wave2 + noise
                    (base * volFactor).coerceIn(0.1f, 1.0f)
                }
                _waveAmplitudes.value = newAmplitudes
            }
        }
    }

    private fun stopWaveAnimation() {
        waveAnimationJob?.cancel()
        _waveAmplitudes.value = List(8) { 0.15f }
    }

    private fun startPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = scope.launch {
            var saveCounter = 0
            while (isActive) {
                delay(if (isBatterySaverMode) 3000L else 1000L)
                exoPlayer?.let { p ->
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    _currentPosition.value = pos
                    val dur = p.duration
                    _totalDuration.value = if (dur > 0L) dur else 0L

                    saveCounter++
                    if (saveCounter % 3 == 0) {
                        val current = _currentStation.value
                        if (current != null && current.isPodcast && pos > 0L) {
                            com.easeaudio.data.PodcastProgressManager.saveProgress(
                                context = context,
                                stationIdOrUrl = current.streamUrl.ifBlank { current.id },
                                positionMs = pos,
                                durationMs = dur.coerceAtLeast(0L),
                                episodeTitle = current.name
                            )
                        }
                    }
                }
            }
        }
    }

    private fun stopPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = null
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs.coerceAtLeast(0L))
        _currentPosition.value = positionMs.coerceAtLeast(0L)
    }

    // startForegroundService() removed (bug #3/#5).
    // MediaSessionService (RadioPlaybackService) starts itself when the MediaSession
    // becomes active and manages its own foreground notification via ExoPlayer's
    // DefaultMediaNotificationProvider. Manually calling startService() here bypassed
    // that, creating a foreground-service-without-notification ANR window on API 26+.

    private fun acquireLocks() {
        try {
            wifiLock?.let { lock ->
                if (!lock.isHeld) {
                    lock.acquire()
                    Log.d(TAG, "WifiLock acquired to prevent WiFi sleep")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WifiLock", e)
        }
    }

    private fun releaseLocks() {
        try {
            wifiLock?.let { lock ->
                if (lock.isHeld) {
                    lock.release()
                    Log.d(TAG, "WifiLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release WifiLock", e)
        }
    }

    fun release() {
        releaseLocks()
        stopWaveAnimation()
        stopPositionPolling()
        artworkFetchJob?.cancel()
        sleepTimerJob?.cancel()
        silentCheckJob?.cancel()
        networkQualityManager.unregisterNetworkCallback()
        // BUG-3 fix: null exoPlayer BEFORE cancelling the scope so that any coroutine
        // woken up from a delay() inside the scope sees null and exits gracefully,
        // rather than crashing with a NullPointerException on a released ExoPlayer.
        exoPlayer?.release()
        exoPlayer = null
        scope.cancel()
        loudnessEnhancer?.release()
        loudnessEnhancer = null
        audioVisualizer?.release()
        audioVisualizer = null
        equalizer?.release()
        equalizer = null
        mediaSession?.release()
        mediaSession = null
        sharedMediaSession = null
        wifiLock = null
        synchronized(RadioPlayerManager::class.java) {
            instance = null
        }
    }


    private fun startSilentChecking() {
        silentCheckJob?.cancel()
        silentCheckJob = scope.launch(Dispatchers.Default) {
            val baseIntervalMs = 30_000L // 30 seconds base
            val maxIntervalMs = 15 * 60 * 1000L // 15 minutes max
            val checkIntervalMs = 15_000L // check every 15 seconds
            
            while (isActive) {
                delay(checkIntervalMs)
                
                // 1. Weak/No internet check protection: skip if completely offline
                if (!networkStatus.value.isConnected) {
                    Log.d(TAG, "No internet connection. Skipping silent checking.")
                    continue
                }
                
                val failedIds = _failedStationIds.value
                if (failedIds.isEmpty()) continue
                
                val currentTime = System.currentTimeMillis()
                val idsToVerify = failedIds.filter { id ->
                    currentTime >= (nextCheckTime[id] ?: 0L)
                }
                
                if (idsToVerify.isEmpty()) continue
                
                // 2. Handle many failed stations safely: limit to a maximum batch of 3 per check
                // and stagger with a 1-second delay between checks to avoid congestion.
                val batchToVerify = idsToVerify.take(3)
                
                for (stationId in batchToVerify) {
                    if (!isActive) break
                    
                    val station = currentStationList.find { it.id == stationId } ?: knownStations[stationId] ?: continue
                    
                    Log.d(TAG, "Starting silent connectivity check for station: ${station.name}")
                    val isCurrentFailingStation = (stationId == _currentStation.value?.id && _playbackError.value != null)
                    val isReachable = if (isCurrentFailingStation) false else checkStreamUrlReachable(station.streamUrl)
                    
                    if (isReachable) {
                        Log.i(TAG, "Station ${station.name} is now reachable silently. Removing from failed set.")
                        withContext(Dispatchers.Main) {
                            _failedStationIds.value = _failedStationIds.value - stationId
                            stationFailureCounts.remove(stationId)
                            nextCheckTime.remove(stationId)
                        }
                    } else {
                        val currentFailureCount = (stationFailureCounts[stationId] ?: 0) + 1
                        stationFailureCounts[stationId] = currentFailureCount
                        
                        val multiplier = Math.min(10, 1 shl (currentFailureCount - 1))
                        val interval = Math.min(maxIntervalMs, baseIntervalMs * multiplier)
                        nextCheckTime[stationId] = currentTime + interval
                        Log.d(TAG, "Station ${station.name} still failing (count: $currentFailureCount). Next check in ${interval / 1000}s.")
                    }
                    
                    // Small delay between checks in the batch
                    delay(1000L)
                }
            }
        }
    }

    /**
     * Bug #4: The previous implementation used GET, which causes the server to start
     * streaming the audio body. For radio streams (infinite body), this hangs until the
     * readTimeout fires on every single check — effectively always waiting 3 seconds.
     *
     * Fix: Try HEAD first (no body transferred). Fall back to GET with a tiny readTimeout
     * for servers that reject HEAD (some ICY/Shoutcast servers return 400/405 for HEAD).
     */
    private suspend fun checkStreamUrlReachable(urlStr: String): Boolean = withContext(Dispatchers.IO) {
        fun tryMethod(method: String): Int? {
            var connection: java.net.HttpURLConnection? = null
            return try {
                var currentUrl = urlStr
                var code: Int? = null
                for (hop in 0 until 3) {
                    val url = java.net.URL(currentUrl)
                    connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                        (this as? javax.net.ssl.HttpsURLConnection)?.apply {
                            sslSocketFactory = com.easeaudio.util.NetworkSecurityHelper.sslSocketFactory
                            hostnameVerifier = com.easeaudio.util.NetworkSecurityHelper.hostnameVerifier
                        }
                    }
                    connection.requestMethod = method
                    connection.connectTimeout = 6000
                    connection.readTimeout = if (method == "HEAD") 6000 else 2000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 NeoTune/1.0")
                    connection.setRequestProperty("Accept", "*/*")
                    
                    val responseCode = connection.responseCode
                    if (responseCode in 300..399) {
                        val location = connection.getHeaderField("Location")
                        connection.disconnect()
                        if (!location.isNullOrBlank()) {
                            currentUrl = location
                            continue
                        }
                    }
                    code = responseCode
                    break
                }
                code
            } catch (e: Exception) {
                Log.d(TAG, "Silent check [$method] failed for $urlStr: ${e.message}")
                null
            } finally {
                connection?.disconnect()
            }
        }

        val headCode = tryMethod("HEAD")
        return@withContext when {
            headCode != null && headCode in 200..399 -> true
            // Server rejected HEAD (400/403/405) — fall back to GET with minimal read
            headCode == null || headCode == 400 || headCode == 403 || headCode == 405 -> {
                val getCode = tryMethod("GET")
                getCode != null && getCode in 200..399
            }
            else -> false
        }
    }
}
