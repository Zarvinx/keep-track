package com.zarvinx.keep_track.db.room

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Query
import androidx.room.Update

data class EpisodeWatchedUpdate(
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "watched")
    val watched: Int
)

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
    @Update(entity = EpisodeEntity::class)
    suspend fun updateEpisodeWatched(update: EpisodeWatchedUpdate): Int

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
