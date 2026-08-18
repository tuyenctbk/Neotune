package com.easeaudio

import android.app.Application
import com.easeaudio.util.NetworkSecurityHelper

class EaseAudioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkSecurityHelper.install()
    }
}


