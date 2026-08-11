package com.easeaudio.data

import android.content.Context

data class PodcastPlaybackProgress(
    val stationIdOrUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val episodeTitle: String = "",
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

object PodcastProgressManager {

    private const val PREFS_NAME = "neotune_podcast_progress_prefs"
    private const val PREFIX_POS = "pos_"
    private const val PREFIX_DUR = "dur_"
    private const val PREFIX_TITLE = "title_"
    private const val PREFIX_TIME = "time_"

    fun saveProgress(
        context: Context,
        stationIdOrUrl: String,
        positionMs: Long,
        durationMs: Long,
        episodeTitle: String = ""
    ) {
        if (stationIdOrUrl.isBlank() || positionMs <= 0L) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong("$PREFIX_POS$stationIdOrUrl", positionMs)
            .putLong("$PREFIX_DUR$stationIdOrUrl", durationMs)
            .putString("$PREFIX_TITLE$stationIdOrUrl", episodeTitle)
            .putLong("$PREFIX_TIME$stationIdOrUrl", System.currentTimeMillis())
            .apply()
    }

    fun getProgress(context: Context, stationIdOrUrl: String): PodcastPlaybackProgress? {
        if (stationIdOrUrl.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val pos = prefs.getLong("$PREFIX_POS$stationIdOrUrl", 0L)
        if (pos <= 0L) return null
        val dur = prefs.getLong("$PREFIX_DUR$stationIdOrUrl", 0L)
        val title = prefs.getString("$PREFIX_TITLE$stationIdOrUrl", "") ?: ""
        val time = prefs.getLong("$PREFIX_TIME$stationIdOrUrl", 0L)
        return PodcastPlaybackProgress(
            stationIdOrUrl = stationIdOrUrl,
            positionMs = pos,
            durationMs = dur,
            episodeTitle = title,
            lastPlayedTimestamp = time
        )
    }

    fun clearProgress(context: Context, stationIdOrUrl: String) {
        if (stationIdOrUrl.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("$PREFIX_POS$stationIdOrUrl")
            .remove("$PREFIX_DUR$stationIdOrUrl")
            .remove("$PREFIX_TITLE$stationIdOrUrl")
            .remove("$PREFIX_TIME$stationIdOrUrl")
            .apply()
    }
}
