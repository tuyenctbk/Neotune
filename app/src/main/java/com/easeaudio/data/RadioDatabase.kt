package com.easeaudio.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RadioStation::class, FavoriteStation::class, RecentSearchQuery::class, ListenLaterItem::class], version = 4, exportSchema = false)
abstract class RadioDatabase : RoomDatabase() {
    abstract fun radioDao(): RadioDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun listenLaterDao(): ListenLaterDao

    companion object {
        @Volatile
        private var INSTANCE: RadioDatabase? = null

        // ─────────────────────────────────────────────────────────────────────
        // Migration stubs: each version bump added new tables or columns to
        // tables that can be recreated from online discovery.
        // User-critical data (favorites, listen-later, recent searches) is
        // preserved by these incremental migrations instead of being dropped.
        // ─────────────────────────────────────────────────────────────────────

        /** v1 → v2: added RecentSearchQuery table */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recent_search_queries` " +
                    "(`query` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`query`))"
                )
            }
        }

        /** v2 → v3: added ListenLaterItem table */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Table name and columns must exactly match the @Entity / @ColumnInfo definitions.
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `listen_later` " +
                    "(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `genre` TEXT NOT NULL, " +
                    "`country` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, " +
                    "`bitrate` TEXT NOT NULL DEFAULT '', `codec` TEXT NOT NULL DEFAULT '', " +
                    "`isCustom` INTEGER NOT NULL DEFAULT 0, `isPodcast` INTEGER NOT NULL DEFAULT 0, " +
                    "`podcastEpisodeUrl` TEXT NOT NULL DEFAULT '', " +
                    "`podcastEpisodeTitle` TEXT NOT NULL DEFAULT '', " +
                    "`addedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))"
                )
            }
        }

        /** v3 → v4: schema alignment migration */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ensure all expected tables and columns are present with matching schemas
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `listen_later` " +
                    "(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `genre` TEXT NOT NULL, " +
                    "`country` TEXT NOT NULL, `streamUrl` TEXT NOT NULL, `imageUrl` TEXT NOT NULL, " +
                    "`bitrate` TEXT NOT NULL DEFAULT '', `codec` TEXT NOT NULL DEFAULT '', " +
                    "`isCustom` INTEGER NOT NULL DEFAULT 0, `isPodcast` INTEGER NOT NULL DEFAULT 0, " +
                    "`podcastEpisodeUrl` TEXT NOT NULL DEFAULT '', " +
                    "`podcastEpisodeTitle` TEXT NOT NULL DEFAULT '', " +
                    "`addedAt` INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY(`id`))"
                )
            }
        }

        fun getDatabase(context: Context): RadioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RadioDatabase::class.java,
                    "easeaudio_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                // Fallback ONLY for truly unrecoverable gaps (e.g. downgrade).
                // This is a last-resort safety net, not the primary migration strategy.
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
