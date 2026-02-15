package com.redcoracle.episodes.db.room

import androidx.room.Dao
import androidx.room.Query

/**
 * DAO for episode watch-state update statements.
 */
@Dao
interface EpisodesRoomDao {
    /**
     * Updates watched state for one episode.
     *
     * @return number of updated rows.
     */
    @Query("UPDATE episodes SET watched = :watched WHERE _id = :episodeId")
    suspend fun updateEpisodeWatched(episodeId: Int, watched: Int): Int

    /**
     * Updates watched state for every episode in one season.
     *
     * @return number of updated rows.
     */
    @Query("UPDATE episodes SET watched = :watched WHERE show_id = :showId AND season_number = :seasonNumber")
    suspend fun updateSeasonWatched(showId: Int, seasonNumber: Int, watched: Int): Int

    /**
     * Marks only already-aired episodes in a season as watched.
     *
     * @return number of updated rows.
     */
    @Query(
        "UPDATE episodes SET watched = 1 " +
            "WHERE show_id = :showId " +
            "AND season_number = :seasonNumber " +
            "AND first_aired <= :nowSeconds " +
            "AND first_aired IS NOT NULL"
    )
    suspend fun markAiredSeasonWatched(showId: Int, seasonNumber: Int, nowSeconds: Long): Int

    /**
     * Updates watched state for all non-special episodes in a show.
     *
     * @return number of updated rows.
     */
    @Query("UPDATE episodes SET watched = :watched WHERE show_id = :showId AND season_number != 0")
    suspend fun updateShowWatched(showId: Int, watched: Int): Int
}
