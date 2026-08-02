package com.easeaudio.data

data class PodcastEpisode(
    val id: String,
    val showId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val pubDate: String,
    val durationMs: Long = 0L,
    val artworkUrl: String = ""
)
