package com.redcoracle.episodes.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

data class ShowDetailsRow(
    val name: String,
    val starred: Int?,
    val archived: Int?,
    val posterPath: String?,
    val overview: String?,
    val firstAired: Long?
)

@Dao
@SkipQueryVerification
interface ShowQueriesDao {
    @Query("SELECT name FROM shows WHERE _id = :showId LIMIT 1")
    fun getShowNameById(showId: Int): String?

    @Query(
        "SELECT name AS name, starred AS starred, archived AS archived, poster_path AS posterPath, " +
            "overview AS overview, first_aired AS firstAired " +
            "FROM shows WHERE _id = :showId LIMIT 1"
    )
    fun getShowDetailsById(showId: Int): ShowDetailsRow?
}
