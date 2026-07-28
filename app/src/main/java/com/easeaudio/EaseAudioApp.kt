package com.easeaudio

import android.app.Application
import android.content.Context
import android.os.Build

class EaseAudioApp : Application() {
    override fun attachBaseContext(base: Context?) {
        val attributed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && base != null) {
            base.createAttributionContext("audio_playback")
        } else {
            base
        }
        super.attachBaseContext(attributed)
    }
}
