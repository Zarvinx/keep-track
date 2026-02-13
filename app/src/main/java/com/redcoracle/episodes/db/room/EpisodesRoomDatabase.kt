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
abstract class EpisodesRoomDatabase : RoomDatabase() {
    abstract fun episodesDao(): EpisodesRoomDao

    companion object {
        @Volatile
        private var instance: EpisodesRoomDatabase? = null

        fun getInstance(context: Context): EpisodesRoomDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EpisodesRoomDatabase::class.java,
                    DatabaseOpenHelper.getDbName()
                ).build().also { instance = it }
            }
        }
    }
}
