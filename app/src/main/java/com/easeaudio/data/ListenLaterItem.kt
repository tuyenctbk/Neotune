package com.easeaudio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listen_later")
data class ListenLaterItem(
    @PrimaryKey
    val id: String,
    val name: String,
    val genre: String,
    val country: String,
    val streamUrl: String,
    val imageUrl: String,
    val bitrate: String = "",
    val codec: String = "",
    val isCustom: Boolean = false,
    val isPodcast: Boolean = false,
    val podcastEpisodeUrl: String = "",
    val podcastEpisodeTitle: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
