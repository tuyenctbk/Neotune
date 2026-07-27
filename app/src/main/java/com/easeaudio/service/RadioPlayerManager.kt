package com.easeaudio.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import com.easeaudio.data.RadioStation
import com.easeaudio.firebase.FirebaseConfigManager
import com.easeaudio.network.NetworkQualityManager
import com.easeaudio.network.NetworkStatus
import com.easeaudio.network.QualityLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.content.ContextCompat

class RadioPlayerManager(private val context: Context) {

    private val TAG = "RadioPlayerManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

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

        // Configure adaptive buffering load control to maintain playback continuity
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                minBuf,
                maxBuf,
                2000,
                4000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        exoPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                volume = _volume.value
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                        _isPlaying.value = isPlayingNow
                        _isLoading.value = false
                        if (isPlayingNow) {
                            acquireLocks()
                            startWaveAnimation()
                            startForegroundService()
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
                            else -> _currentStation.value?.name ?: "Live Audio Stream"
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Playback error: ${error.message}", error)
                        _isLoading.value = false
                        _isPlaying.value = false
                        _playbackError.value = "Unable to connect to stream. Please check network or try another station."
                        stopWaveAnimation()
                    }
                })
            }

        exoPlayer?.let { player ->
            try {
                mediaSession = MediaSession.Builder(context, player).build()
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
                    _playbackError.value = "Waiting for network reconnection..."
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
                if (player.playbackState == Player.STATE_ENDED || player.mediaItemCount == 0) {
                    _currentStation.value?.let { playStation(it) }
                } else {
                    player.play()
                }
            }
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
            // Timer expired -> fade out and stop playback
            for (i in 10 downTo 0) {
                exoPlayer?.volume = (_volume.value * (i / 10.0f))
                delay(150L)
            }
            exoPlayer?.pause()
            exoPlayer?.volume = _volume.value
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
                delay(120L)
                val newAmplitudes = List(8) {
                    (0.15f + Math.random().toFloat() * 0.8f)
                }
                _waveAmplitudes.value = newAmplitudes
            }
        }
    }

    private fun stopWaveAnimation() {
        waveAnimationJob?.cancel()
        _waveAmplitudes.value = List(8) { 0.15f }
    }

    private fun startForegroundService() {
        try {
            val serviceIntent = Intent(context, RadioPlaybackService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start RadioPlaybackService: ${e.message}")
        }
    }

    private fun acquireLocks() {
        try {
            if (wakeLock == null) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Tunora:RadioPlaybackWakeLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L)
            }

            if (wifiLock == null) {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Tunora:RadioPlaybackWifiLock")
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake/wifi lock: ${e.message}")
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing locks: ${e.message}")
        }
    }

    fun release() {
        releaseLocks()
        stopWaveAnimation()
        sleepTimerJob?.cancel()
        networkQualityManager.unregister()
        scope.cancel()
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun CharSequence?.isNull_Blank(): Boolean = this == null || this.isBlank()
}
