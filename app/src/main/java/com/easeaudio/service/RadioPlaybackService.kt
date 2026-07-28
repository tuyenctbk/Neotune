package com.easeaudio.service

import android.content.Context
import android.os.Build
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioPlaybackService : MediaSessionService() {
    override fun attachBaseContext(base: Context?) {
        val attributed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && base != null) {
            base.createAttributionContext("audio_playback")
        } else {
            base
        }
        super.attachBaseContext(attributed)
    }

    override fun onCreate() {
        super.onCreate()
        val playerManager = RadioPlayerManager.getInstance(this)
        RadioPlayerManager.sharedMediaSession?.let { session ->
            addSession(session)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        RadioPlayerManager.getInstance(this)
        return RadioPlayerManager.sharedMediaSession
    }

    override fun onDestroy() {
        RadioPlayerManager.getInstance(this).release()
        super.onDestroy()
    }
}
