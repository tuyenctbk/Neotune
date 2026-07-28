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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.easeaudio.data.RadioStation
import com.easeaudio.firebase.FirebaseConfigManager
import com.easeaudio.network.NetworkQualityManager
import com.easeaudio.network.NetworkStatus
import android.os.Build
import kotlin.random.Random
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RadioPlayerManager(private val context: Context) {
    private val attributionContext: Context by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            context.createAttributionContext("audio_playback")
        } else {
            context
        }
    }







    companion object {
        @Volatile
        var sharedMediaSession: MediaSession? = null

        @Volatile
        private var instance: RadioPlayerManager? = null

        fun getInstance(context: Context): RadioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: RadioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val TAG = "RadioPlayerManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())




    val networkQualityManager = NetworkQualityManager(context)
    val firebaseConfigManager = FirebaseConfigManager(context)

    val networkStatus: StateFlow<NetworkStatus> = networkQualityManager.networkStatus
    val remoteConfig = firebaseConfigManager.configState

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val _currentStation = MutableStateFlow<RadioStation?>(null)
    val currentStation: StateFlow<RadioStation?> = _currentStation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

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

    // Visualizer simulated wave amplitudes (8 bars)
    private val _waveAmplitudes = MutableStateFlow(List(8) { 0.2f })
    val waveAmplitudes: StateFlow<List<Float>> = _waveAmplitudes.asStateFlow()

    // Volume level 0.0f to 1.0f
    private val _volume = MutableStateFlow(0.85f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Sleep Timer
    private val _sleepTimerMinutesRemaining = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesRemaining: StateFlow<Int?> = _sleepTimerMinutesRemaining.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var waveAnimationJob: Job? = null

    init {
        setupPlayer()
        observeNetworkChanges()
        startSilentChecking()
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

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

        exoPlayer = ExoPlayer.Builder(attributionContext)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                volume = _volume.value
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _isPlaying.value = isPlayingNow
                        _isLoading.value = false
                        if (isPlayingNow) {
                            // Bug #3/#5: Do NOT call startForegroundService() here.
                            // MediaSessionService owns the foreground notification lifecycle.
                            // Calling startService() from inside the manager bypasses that,
                            // creating a foreground-service-without-notification window (ANR risk).
                            acquireLocks()
                            startWaveAnimation()
                            _currentStation.value?.let { current ->
                                if (_failedStationIds.value.contains(current.id)) {
                                    _failedStationIds.value = _failedStationIds.value - current.id
                                }
                            }
                        } else {
                            releaseLocks()
                            stopWaveAnimation()
                        }
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                _isLoading.value = true
                                _playbackError.value = null
                            }
                            Player.STATE_READY -> {
                                _isLoading.value = false
                                _playbackError.value = null
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
                        _streamTitle.value = when {
                            !title.isNull_Blank() && !artist.isNull_Blank() -> "$artist - $title"
                            !title.isNull_Blank() -> title
                            !artist.isNull_Blank() -> artist
                            else -> _currentStation.value?.name ?: context.getString(R.string.live_audio_stream)
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        _isLoading.value = false
                        _isPlaying.value = false
                        val isConnected = networkStatus.value.isConnected
                        if (isConnected) {
                            _currentStation.value?.let { current ->
                                _failedStationIds.value = _failedStationIds.value + current.id
                            }
                            _playbackError.value = context.getString(R.string.unable_connect_error)
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

                val mediaSessionCallback = object : MediaSession.Callback {
                    override fun onPlayerCommandRequest(
                        session: MediaSession,
                        controller: MediaSession.ControllerInfo,
                        playerCommand: Int
                    ): Int {
                        if (playerCommand == Player.COMMAND_SEEK_TO_NEXT) {
                            scope.launch(Dispatchers.Main) {
                                playNextStation(currentStationList)
                            }
                            return SessionError.ERROR_NOT_SUPPORTED
                        }
                        if (playerCommand == Player.COMMAND_SEEK_TO_PREVIOUS) {
                            scope.launch(Dispatchers.Main) {
                                playPreviousStation(currentStationList)
                            }
                            return SessionError.ERROR_NOT_SUPPORTED
                        }
                        return super.onPlayerCommandRequest(session, controller, playerCommand)
                    }
                }

                mediaSession = MediaSession.Builder(attributionContext, player)
                    .setSessionActivity(pendingIntent)
                    .setCallback(mediaSessionCallback)
                    .build().also {
                        sharedMediaSession = it
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create MediaSession: ${e.message}", e)
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

    fun playStation(station: RadioStation) {
        _currentStation.value = station
        _playbackError.value = null
        _isLoading.value = true
        _streamTitle.value = station.name
        // Bug #5: Removed startForegroundService() call. MediaSessionService handles
        // starting itself when the MediaSession becomes active via ExoPlayer.play().

        exoPlayer?.let { player ->
            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist(station.genre)
                .setArtworkUri(Uri.parse(station.imageUrl))
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(station.streamUrl)
                .setMediaMetadata(mediaMetadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                // Bug #6: mediaItemCount is always >= 1 after the first playStation() call
                // because ExoPlayer retains the last item. The correct sentinel for
                // "nothing has ever been played" is _currentStation.value == null.
                if (_currentStation.value == null || player.playbackState == Player.STATE_ENDED || _playbackError.value != null) {
                    _currentStation.value?.let { playStation(it) }
                } else {
                    player.play()
                }
            }
        }
    }

    fun retryCurrentStation() {
        _currentStation.value?.let { playStation(it) }
    }

    fun playNextStation(stationList: List<RadioStation>) {
        if (stationList.isEmpty()) return
        val current = _currentStation.value ?: return playStation(stationList.first())
        val currentIndex = stationList.indexOfFirst { it.id == current.id }
        val nextIndex = if (currentIndex >= 0 && currentIndex < stationList.size - 1) currentIndex + 1 else 0
        playStation(stationList[nextIndex])
    }

    fun playPreviousStation(stationList: List<RadioStation>) {
        if (stationList.isEmpty()) return
        val current = _currentStation.value ?: return playStation(stationList.last())
        val currentIndex = stationList.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else stationList.size - 1
        playStation(stationList[prevIndex])
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

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesRemaining.value = null
    }

    private fun startWaveAnimation() {
        waveAnimationJob?.cancel()
        waveAnimationJob = scope.launch {
            while (isActive && _isPlaying.value) {
                delay(400L)
                // Bug #8: Use Kotlin Random instead of Java Math.random()
                val newAmplitudes = List(8) { 0.15f + Random.nextFloat() * 0.8f }
                _waveAmplitudes.value = newAmplitudes
            }
        }
    }

    private fun stopWaveAnimation() {
        waveAnimationJob?.cancel()
        _waveAmplitudes.value = List(8) { 0.15f }
    }

    // startForegroundService() removed (bug #3/#5).
    // MediaSessionService (RadioPlaybackService) starts itself when the MediaSession
    // becomes active and manages its own foreground notification via ExoPlayer's
    // DefaultMediaNotificationProvider. Manually calling startService() here bypassed
    // that, creating a foreground-service-without-notification ANR window on API 26+.

    private fun acquireLocks() {
        // Handled by ExoPlayer C.WAKE_MODE_NETWORK
    }

    private fun releaseLocks() {
        // Handled by ExoPlayer
    }

    fun release() {
        releaseLocks()
        stopWaveAnimation()
        sleepTimerJob?.cancel()
        silentCheckJob?.cancel()
        networkQualityManager.unregister()
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        sharedMediaSession = null
        exoPlayer?.release()
        exoPlayer = null
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
                    val isReachable = checkStreamUrlReachable(station.streamUrl)
                    
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
                val url = java.net.URL(urlStr)
                connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = method
                connection.connectTimeout = 5000
                // HEAD: no body; GET: read nothing (just need response code)
                connection.readTimeout = if (method == "HEAD") 5000 else 1000
                connection.instanceFollowRedirects = true
                connection.responseCode
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
            // Server rejected HEAD (400/405) — fall back to GET with minimal read
            headCode == null || headCode == 400 || headCode == 405 -> {
                val getCode = tryMethod("GET")
                getCode != null && getCode in 200..399
            }
            else -> false
        }
    }

    private fun CharSequence?.isNull_Blank(): Boolean = this == null || this.isBlank()
}
