package com.easeaudio.data

data class EpisodeChapter(
    val title: String,
    val startTimeMs: Long,
    val durationText: String = "",
    val imageUrl: String? = null,
    val url: String? = null
)

data class PodcastEpisode(
    val id: String,
    val showId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val pubDate: String,
    val durationMs: Long = 0L,
    val artworkUrl: String = "",
    val chaptersUrl: String? = null,
    val transcriptUrl: String? = null,
    val chapters: List<EpisodeChapter> = emptyList()
)
