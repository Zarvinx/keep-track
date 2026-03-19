package com.zarvinx.keep_track.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BackupDao {
    @Query("SELECT * FROM shows")
    fun getAllShows(): List<ShowEntity>

    @Query("SELECT * FROM episodes")
    fun getAllEpisodes(): List<EpisodeEntity>

    @Query("DELETE FROM episodes")
    fun deleteAllEpisodes()

    @Query("DELETE FROM shows")
    fun deleteAllShows()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertShow(show: ShowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEpisode(episode: EpisodeEntity)
}
