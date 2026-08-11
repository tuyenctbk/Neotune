package com.easeaudio.service

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var analytics: FirebaseAnalytics? = null
    private var activeStreamTrace: Trace? = null
    private var traceStartTimeMs: Long = 0L
    private var currentTracingStation: String? = null

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

    /**
     * Start a Firebase Performance trace to monitor radio stream buffering startup time & latency.
     */
    fun startStreamBufferingTrace(stationName: String) {
        try {
            if (activeStreamTrace != null) {
                stopStreamBufferingTrace(success = false, reason = "New station selected")
            }
            traceStartTimeMs = System.currentTimeMillis()
            currentTracingStation = stationName
            val sanitizedName = stationName.take(32).replace(Regex("[^a-zA-Z0-9_]"), "_")
            activeStreamTrace = FirebasePerformance.getInstance().newTrace("stream_buffering_latency").apply {
                putAttribute("station_name", sanitizedName)
                start()
            }
            Log.d(TAG, "Started Firebase Performance trace for stream: $stationName")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start Firebase Perf trace", e)
        }
    }

    /**
     * Stop the Firebase Performance trace when stream starts playing or fails.
     */
    fun stopStreamBufferingTrace(success: Boolean, reason: String = "Normal") {
        try {
            activeStreamTrace?.let { trace ->
                val latencyMs = System.currentTimeMillis() - traceStartTimeMs
                trace.putAttribute("buffering_success", success.toString())
                trace.putAttribute("stop_reason", reason)
                trace.putMetric("latency_ms", latencyMs)
                trace.stop()

                val bundle = Bundle().apply {
                    putString("station_name", currentTracingStation ?: "Unknown")
                    putBoolean("success", success)
                    putLong("latency_ms", latencyMs)
                    putString("reason", reason)
                }
                logEvent("radio_stream_buffering_perf", bundle)
                Log.d(TAG, "Stopped Firebase Perf trace ($latencyMs ms, success=$success, station=$currentTracingStation)")
            }
            activeStreamTrace = null
            currentTracingStation = null
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to stop Firebase Perf trace", e)
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
