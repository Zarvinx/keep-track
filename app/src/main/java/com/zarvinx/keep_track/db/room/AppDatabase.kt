package com.zarvinx.keep_track.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EpisodeEntity::class, ShowEntity::class],
    version = 14,
    exportSchema = true
)
/**
 * App-wide Room database entrypoint.
 *
 * This singleton opens the app's existing SQLite database file through Room.
 */
abstract class AppDatabase : RoomDatabase() {
    /**
     * DAO for watch-state update statements.
     */
    abstract fun episodesDao(): EpisodesRoomDao

    /**
     * DAO for add-show insert and duplicate-lookup statements.
     */
    abstract fun addShowDao(): AddShowRoomDao

    /**
     * DAO for bulk refresh reconciliation statements.
     */
    abstract fun refreshShowDao(): RefreshShowRoomDao

    /**
     * DAO for show mutation statements.
     */
    abstract fun showMutationsDao(): ShowMutationsDao

    /**
     * DAO for focused show detail lookups.
     */
    abstract fun showQueriesDao(): ShowQueriesDao

    /**
     * DAO for read models used by list/detail view models.
     */
    abstract fun appReadDao(): AppReadDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * Returns a singleton Room database instance bound to the app DB file.
         */
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    AppDatabaseFile.resolveDbName()
                )
                    .build()
                    .also { instance = it }
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
