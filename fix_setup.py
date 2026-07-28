import re

path = 'app/src/main/java/com/easeaudio/service/RadioPlayerManager.kt'
with open(path, 'r') as f:
    content = f.read()

setup_player = """    @OptIn(UnstableApi::class)
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
                            startForegroundService()
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
"""

start_pattern = r'    @OptIn\(UnstableApi::class\)\n    private fun setupPlayer\(\) \{.*?\n        exoPlayer\?\.let \{ player ->'

# Replace everything from setupPlayer until exoPlayer?.let with the right setup_player content + '        exoPlayer?.let { player ->'
new_content = re.sub(start_pattern, setup_player + '\n        exoPlayer?.let { player ->', content, flags=re.DOTALL)

with open(path, 'w') as f:
    f.write(new_content)
