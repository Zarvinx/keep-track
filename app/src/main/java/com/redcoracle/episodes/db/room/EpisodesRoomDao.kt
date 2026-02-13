package com.redcoracle.episodes.db.room

import androidx.room.Dao
import androidx.room.Query

@Dao
interface EpisodesRoomDao {
    @Query("UPDATE episodes SET watched = :watched WHERE _id = :episodeId")
    suspend fun updateEpisodeWatched(episodeId: Int, watched: Int): Int

    @Query("UPDATE episodes SET watched = :watched WHERE show_id = :showId AND season_number = :seasonNumber")
    suspend fun updateSeasonWatched(showId: Int, seasonNumber: Int, watched: Int): Int

    @Query(
        "UPDATE episodes SET watched = 1 " +
            "WHERE show_id = :showId " +
            "AND season_number = :seasonNumber " +
            "AND first_aired <= :nowSeconds " +
            "AND first_aired IS NOT NULL"
    )
    suspend fun markAiredSeasonWatched(showId: Int, seasonNumber: Int, nowSeconds: Long): Int

    @Query("UPDATE episodes SET watched = :watched WHERE show_id = :showId AND season_number != 0")
    suspend fun updateShowWatched(showId: Int, watched: Int): Int
}
