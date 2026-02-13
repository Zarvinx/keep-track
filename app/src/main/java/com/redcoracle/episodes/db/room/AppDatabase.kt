package com.redcoracle.episodes.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.redcoracle.episodes.db.DatabaseOpenHelper

@Database(
    entities = [EpisodeEntity::class],
    version = 10,
    exportSchema = false
)
/**
 * App-wide Room database entrypoint.
 *
 * The singleton normalizes legacy `episodes` table declarations before opening Room so
 * schema validation passes without legacy fallback write paths.
 */
abstract class AppDatabase : RoomDatabase() {
    abstract fun episodesDao(): EpisodesRoomDao
    abstract fun addShowDao(): AddShowRoomDao
    abstract fun refreshShowDao(): RefreshShowRoomDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                LegacySchemaNormalizer.normalizeEpisodesTable(context.applicationContext)
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseOpenHelper.getDbName()
                ).build().also { instance = it }
            }
        }

        /**
         * Flushes WAL pages to the main DB file before file-copy backups.
         */
        fun checkpoint(context: Context) {
            runCatching {
                val db = getInstance(context)
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
            }
        }

        /**
         * Closes and clears the singleton so restore operations can safely replace DB files.
         */
        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
