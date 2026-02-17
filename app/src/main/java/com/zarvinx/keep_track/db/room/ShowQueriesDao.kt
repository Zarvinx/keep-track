package com.zarvinx.keep_track.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

/**
 * Projection for show details used by the show detail screen.
 */
data class ShowDetailsRow(
    val name: String,
    val starred: Int?,
    val archived: Int?,
    val posterPath: String?,
    val overview: String?,
    val firstAired: Long?
)

/**
 * DAO exposing focused show-level lookups.
 */
@Dao
@SkipQueryVerification
interface ShowQueriesDao {
    /**
     * Returns the show name for one id, or null when missing.
     */
    @Query("SELECT name FROM shows WHERE _id = :showId LIMIT 1")
    fun getShowNameById(showId: Int): String?

    /**
     * Returns a show details projection for one id, or null when missing.
     */
    @Query(
        "SELECT name AS name, starred AS starred, archived AS archived, poster_path AS posterPath, " +
            "overview AS overview, first_aired AS firstAired " +
            "FROM shows WHERE _id = :showId LIMIT 1"
    )
    fun getShowDetailsById(showId: Int): ShowDetailsRow?
}
