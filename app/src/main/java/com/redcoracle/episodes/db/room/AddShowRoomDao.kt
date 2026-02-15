package com.redcoracle.episodes.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

/**
 * DAO used by add-show flows to check duplicates and insert show/episode rows.
 */
@Dao
@SkipQueryVerification
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

    /**
     * Inserts a show row and returns the inserted row id.
     */
    @Query(
        "INSERT INTO shows (" +
            "tvdb_id, tmdb_id, imdb_id, name, language, overview, first_aired, banner_path, fanart_path, poster_path" +
            ") VALUES (" +
            ":tvdbId, :tmdbId, :imdbId, :name, :language, :overview, :firstAired, :bannerPath, :fanartPath, :posterPath" +
            ")"
    )
    fun insertShow(
        tvdbId: Int?,
        tmdbId: Int,
        imdbId: String?,
        name: String,
        language: String?,
        overview: String?,
        firstAired: Long?,
        bannerPath: String?,
        fanartPath: String?,
        posterPath: String?
    ): Long

    /**
     * Inserts an episode row and returns the inserted row id.
     */
    @Query(
        "INSERT INTO episodes (" +
            "tvdb_id, tmdb_id, imdb_id, show_id, name, language, overview, episode_number, season_number, first_aired" +
            ") VALUES (" +
            ":tvdbId, :tmdbId, :imdbId, :showId, :name, :language, :overview, :episodeNumber, :seasonNumber, :firstAired" +
            ")"
    )
    fun insertEpisode(
        tvdbId: Int?,
        tmdbId: Int?,
        imdbId: String?,
        showId: Int,
        name: String,
        language: String?,
        overview: String?,
        episodeNumber: Int?,
        seasonNumber: Int?,
        firstAired: Long?
    ): Long
}
