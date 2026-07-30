package com.easeaudio.service

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var analytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            analytics = FirebaseAnalytics.getInstance(context)
            // Enable Crashlytics collection
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            // Enable Analytics collection
            analytics?.setAnalyticsCollectionEnabled(true)
            Log.d(TAG, "Firebase Analytics & Crashlytics initialized safely")
        } catch (e: Throwable) {
            Log.e(TAG, "Error in Firebase initialization", e)
        }
    }

    fun logScreenView(screenName: String, screenClass: String = "MainActivity") {
        try {
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            Log.d(TAG, "Logged screen: $screenName")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to log screen", e)
        }
    }

    fun logEvent(eventName: String, params: Bundle? = null) {
        try {
            analytics?.logEvent(eventName, params)
            Log.d(TAG, "Logged event: $eventName")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to log event", e)
        }
    }
    
    fun recordException(throwable: Throwable) {
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to record exception", e)
        }
    }
}
