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
 * Transitional Room database for watch-state write migration.
 *
 * This database intentionally opens the existing legacy file `episodes.db` to support
 * incremental adoption. During this phase, callers must be resilient to schema-validation
 * failures on older installs (see EpisodeWatchStateWriter fallback paths).
 */
abstract class AppDatabase : RoomDatabase() {
    abstract fun episodesDao(): EpisodesRoomDao
    abstract fun addShowDao(): AddShowRoomDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseOpenHelper.getDbName()
                ).build().also { instance = it }
            }
        }
    }
}
