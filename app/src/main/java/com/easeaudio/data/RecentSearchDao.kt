package com.easeaudio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_search_queries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearchQueries(limit: Int = 10): Flow<List<RecentSearchQuery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(searchQuery: RecentSearchQuery)

    @Query("DELETE FROM recent_search_queries WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM recent_search_queries")
    suspend fun clearRecentSearchQueries()
}
