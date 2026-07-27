package com.easeaudio.service

import android.content.Context
import android.util.Log

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    fun initialize(context: Context) {
        try {
            Log.d(TAG, "EaseAudio initialized safely")
        } catch (e: Throwable) {
            Log.e(TAG, "Error in initialization", e)
        }
    }
}
