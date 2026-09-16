package com.easeaudio.service

import android.util.Log
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession

class RadioPlaybackService : MediaLibraryService() {

    // No attachBaseContext override: the Application class already applies the attribution
    // context, and RadioPlayerManager builds its own attributionContext from applicationContext.
    // A second override here would create a mismatched attributed context for the Service,
    // causing subtle PendingIntent resolution failures on some OEMs.

    override fun onCreate() {
        super.onCreate()
        // Ensure the singleton PlayerManager is alive and register its session with this service.
        // RadioPlayerManager.getInstance() initialises the ExoPlayer + MediaLibrarySession
        // the first time it is called. Subsequent calls return the cached singleton.
        RadioPlayerManager.getInstance(applicationContext)
        val session = RadioPlayerManager.sharedMediaLibrarySession
        if (session != null) {
            addSession(session)
            Log.d("RadioPlaybackService", "MediaLibrarySession added to service (id=${session.id})")
        } else {
            Log.w("RadioPlaybackService", "MediaLibrarySession was null on onCreate — will be attached via onGetSession")
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        // Initialise on demand if OS restarted the service independently.
        RadioPlayerManager.getInstance(applicationContext)
        val session = RadioPlayerManager.sharedMediaLibrarySession
        // Lazily add the session if it wasn't available during onCreate.
        if (session != null && !getSessions().contains(session)) {
            addSession(session)
            Log.d("RadioPlaybackService", "MediaLibrarySession lazily attached in onGetSession (id=${session.id})")
        }
        return session
    }

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        try {
            val playerManager = RadioPlayerManager.getInstance(applicationContext)
            playerManager.stopPlayer()
        } catch (e: Exception) {
            Log.w("RadioPlaybackService", "Error stopping player on task removed: ${e.message}")
        }
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // Remove sessions from this service so AAOS / Auto does not hold stale references.
        // The shared MediaLibrarySession itself is owned by RadioPlayerManager and must NOT
        // be released here — only removed from the service's session set.
        try {
            getSessions().toList().forEach { session ->
                removeSession(session)
            }
        } catch (e: Exception) {
            Log.w("RadioPlaybackService", "Error removing sessions on destroy: ${e.message}")
        }
        // MediaLibraryService.super.onDestroy() cleans up its own internal session state.
        super.onDestroy()
    }
}
