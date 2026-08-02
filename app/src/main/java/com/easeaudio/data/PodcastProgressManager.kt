package com.easeaudio.data

import android.content.Context

object PodcastProgressManager {

    private const val PREFS_NAME = "neotune_podcast_progress_prefs"
    private const val PREFIX_POSITION = "podcast_pos_"

    fun saveProgress(context: Context, stationId: String, positionMs: Long) {
        if (stationId.isBlank() || positionMs <= 0L) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong("$PREFIX_POSITION$stationId", positionMs).apply()
    }

    fun getProgress(context: Context, stationId: String): Long {
        if (stationId.isBlank()) return 0L
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong("$PREFIX_POSITION$stationId", 0L)
    }

    fun clearProgress(context: Context, stationId: String) {
        if (stationId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$PREFIX_POSITION$stationId").apply()
    }
}
