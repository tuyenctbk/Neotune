package com.easeaudio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "radio_stations")
data class RadioStation(
    @PrimaryKey val id: String,
    val name: String,
    val genre: String,
    val country: String,
    val streamUrl: String,
    val imageUrl: String,
    val bitrate: String = "128 kbps",
    val codec: String = "AAC/MP3",
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false,
    val lastListenedTimestamp: Long = 0L
) {
    val isPodcast: Boolean
        get() = genre.contains("Podcast", ignoreCase = true) ||
                genre.contains("Audiobook", ignoreCase = true) ||
                genre.contains("Story", ignoreCase = true) ||
                genre.contains("Interview", ignoreCase = true) ||
                genre.contains("Drama", ignoreCase = true) ||
                (genre.contains("Talk", ignoreCase = true) && !genre.contains("News", ignoreCase = true) && !genre.contains("Radio", ignoreCase = true))
}
