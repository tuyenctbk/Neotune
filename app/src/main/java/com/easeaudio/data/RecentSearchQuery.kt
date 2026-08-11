package com.easeaudio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_search_queries")
data class RecentSearchQuery(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
