package com.zarvinx.keep_track.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

/**
 * Lightweight projection of an episode row used while reconciling refresh results.
 */
data class ExistingEpisodeRow(
    val id: Int,
    val tmdbId: Int?,
    val seasonNumber: Int?,
    val episodeNumber: Int?
)

/**
 * Room DAO for updating a show and reconciling its episode list during refresh.
 *
 * All methods are synchronous and are expected to be called from background threads.
 */
@Dao
@SkipQueryVerification
interface RefreshShowRoomDao {
    /**
     * Updates core metadata fields for an existing show row.
     *
     * @return number of updated rows.
     */
    @Query(
        "UPDATE shows SET " +
            "tvdb_id = :tvdbId, tmdb_id = :tmdbId, imdb_id = :imdbId, name = :name, language = :language, " +
            "overview = :overview, first_aired = :firstAired, banner_path = :bannerPath, fanart_path = :fanartPath, " +
            "poster_path = :posterPath, status = :status " +
            "WHERE _id = :showId"
    )
    fun updateShow(
        showId: Int,
        tvdbId: Int?,
        tmdbId: Int,
        imdbId: String?,
        name: String,
        language: String?,
        overview: String?,
        firstAired: Long?,
        bannerPath: String?,
        fanartPath: String?,
        posterPath: String?,
        status: String?
    ): Int

    /**
     * Returns the current persisted episodes for a show so callers can diff remote results.
     */
    @Query(
        "SELECT _id AS id, tmdb_id AS tmdbId, season_number AS seasonNumber, episode_number AS episodeNumber " +
            "FROM episodes WHERE show_id = :showId"
    )
    fun getEpisodesForShow(showId: Int): List<ExistingEpisodeRow>

    /**
     * Updates one existing episode row.
     *
     * @return number of updated rows.
     */
    @Query(
        "UPDATE episodes SET " +
            "show_id = :showId, tvdb_id = :tvdbId, tmdb_id = :tmdbId, imdb_id = :imdbId, name = :name, " +
            "language = :language, overview = :overview, episode_number = :episodeNumber, " +
            "season_number = :seasonNumber, first_aired = :firstAired " +
            "WHERE _id = :episodeId"
    )
    fun updateEpisode(
        episodeId: Int,
        showId: Int,
        tvdbId: Int?,
        tmdbId: Int?,
        imdbId: String?,
        name: String,
        language: String?,
        overview: String?,
        episodeNumber: Int?,
        seasonNumber: Int?,
        firstAired: Long?
    ): Int

    /**
     * Inserts a new episode row for the given show.
     *
     * @return inserted row id.
     */
    @Query(
        "INSERT INTO episodes (" +
            "show_id, tvdb_id, tmdb_id, imdb_id, name, language, overview, episode_number, season_number, first_aired" +
            ") VALUES (" +
            ":showId, :tvdbId, :tmdbId, :imdbId, :name, :language, :overview, :episodeNumber, :seasonNumber, :firstAired" +
            ")"
    )
    fun insertEpisode(
        showId: Int,
        tvdbId: Int?,
        tmdbId: Int?,
        imdbId: String?,
        name: String,
        language: String?,
        overview: String?,
        episodeNumber: Int?,
        seasonNumber: Int?,
        firstAired: Long?
    ): Long

    /**
     * Deletes one episode by primary key.
     *
     * @return number of deleted rows.
     */
    @Query("DELETE FROM episodes WHERE _id = :episodeId")
    fun deleteEpisodeById(episodeId: Int): Int
}
