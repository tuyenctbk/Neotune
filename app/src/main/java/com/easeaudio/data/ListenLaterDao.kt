package com.easeaudio.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListenLaterDao {
    @Query("SELECT * FROM listen_later ORDER BY addedAt DESC")
    fun getAllListenLater(): Flow<List<ListenLaterItem>>

    @Query("SELECT EXISTS(SELECT 1 FROM listen_later WHERE id = :id)")
    fun isListenLater(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM listen_later WHERE id = :id)")
    suspend fun isListenLaterDirect(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ListenLaterItem)

    @Query("DELETE FROM listen_later WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM listen_later")
    suspend fun clearAll()
}
