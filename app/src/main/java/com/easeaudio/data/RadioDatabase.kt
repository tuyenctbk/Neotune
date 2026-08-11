package com.easeaudio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RadioStation::class, FavoriteStation::class, RecentSearchQuery::class], version = 3, exportSchema = false)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun radioDao(): RadioDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentSearchDao(): RecentSearchDao

    companion object {
        @Volatile
        private var INSTANCE: RadioDatabase? = null

        fun getDatabase(context: Context): RadioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RadioDatabase::class.java,
                    "easeaudio_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
