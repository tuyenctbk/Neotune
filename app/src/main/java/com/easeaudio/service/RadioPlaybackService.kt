package com.easeaudio.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class RadioPlaybackService : MediaSessionService() {

    // No attachBaseContext override: the Application class already applies the attribution
    // context, and RadioPlayerManager builds its own attributionContext from applicationContext.
    // A second override here would create a mismatched attributed context for the Service,
    // causing subtle PendingIntent resolution failures on some OEMs.

    override fun onCreate() {
        super.onCreate()
        // Ensure singleton is alive and register its session with this service.
        RadioPlayerManager.getInstance(applicationContext)
        RadioPlayerManager.sharedMediaSession?.let { session ->
            addSession(session)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // Initialise on demand if OS restarted the service independently.
        RadioPlayerManager.getInstance(applicationContext)
        return RadioPlayerManager.sharedMediaSession
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        try {
            val playerManager = RadioPlayerManager.getInstance(applicationContext)
            playerManager.stopPlayer()
        } catch (e: Exception) {
            android.util.Log.w("RadioPlaybackService", "Error stopping player on task removed: ${e.message}")
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Do NOT release the singleton here. The PlayerManager is shared with the ViewModel
        // and Activity. Releasing it inside the service would null sharedMediaSession and
        // ExoPlayer while the UI is still active, causing crashes on re-interaction.
        // MediaSessionService.super.onDestroy() cleans up its own internal session state.
        super.onDestroy()
    }
}
