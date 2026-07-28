package com.easeaudio.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioPlaybackService : MediaSessionService() {

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

