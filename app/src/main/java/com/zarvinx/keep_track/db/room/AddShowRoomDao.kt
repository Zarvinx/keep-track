package com.zarvinx.keep_track.db.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * DAO used by add-show flows to check duplicates and insert show/episode rows.
 */
@Dao
interface AddShowRoomDao {
    /**
     * Returns a show id matching a TMDB id, or null if no match exists.
     */
    @Query("SELECT _id FROM shows WHERE tmdb_id = :tmdbId LIMIT 1")
    fun findShowIdByTmdbId(tmdbId: Int): Int?

    /**
     * Returns a show id matching a TVDB id, or null if no match exists.
     */
    @Query("SELECT _id FROM shows WHERE tvdb_id = :tvdbId LIMIT 1")
    fun findShowIdByTvdbId(tvdbId: Int): Int?

    /**
     * Returns a show id matching an IMDb id, or null if no match exists.
     */
    @Query("SELECT _id FROM shows WHERE imdb_id = :imdbId LIMIT 1")
    fun findShowIdByImdbId(imdbId: String): Int?

    @Insert
    fun insertShow(show: ShowEntity): Long

    @Insert
    fun insertEpisode(episode: EpisodeEntity): Long
}
