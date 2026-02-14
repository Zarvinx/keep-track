package com.redcoracle.episodes.db.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.redcoracle.episodes.db.DatabaseOpenHelper

@Database(
    entities = [EpisodeEntity::class, ShowEntity::class],
    version = 11,
    exportSchema = false
)
/**
 * App-wide Room database entrypoint.
 *
 * This singleton opens the app's existing SQLite database file through Room.
 */
abstract class AppDatabase : RoomDatabase() {
    abstract fun episodesDao(): EpisodesRoomDao
    abstract fun addShowDao(): AddShowRoomDao
    abstract fun refreshShowDao(): RefreshShowRoomDao
    abstract fun showMutationsDao(): ShowMutationsDao
    abstract fun showQueriesDao(): ShowQueriesDao
    abstract fun appReadDao(): AppReadDao

    companion object {
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No physical table change is required. This migration exists to
                // advance Room's identity hash after registering ShowEntity.
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                RoomSchemaNormalizer.normalizeIfNeeded(context.applicationContext)
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DatabaseOpenHelper.getDbName()
                )
                    .addMigrations(MIGRATION_10_11)
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
