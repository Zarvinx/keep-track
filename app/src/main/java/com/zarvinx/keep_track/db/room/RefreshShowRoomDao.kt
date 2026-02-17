package com.zarvinx.keep_track.db.room

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Lightweight projection of an episode row used while reconciling refresh results.
 */
data class ExistingEpisodeRow(
    val id: Int,
    val tmdbId: Int?,
    val seasonNumber: Int?,
    val episodeNumber: Int?
)

data class RefreshShowUpdate(
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "tvdb_id")
    val tvdbId: Int?,
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int,
    @ColumnInfo(name = "imdb_id")
    val imdbId: String?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "language")
    val language: String?,
    @ColumnInfo(name = "overview")
    val overview: String?,
    @ColumnInfo(name = "first_aired")
    val firstAired: Long?,
    @ColumnInfo(name = "banner_path")
    val bannerPath: String?,
    @ColumnInfo(name = "fanart_path")
    val fanartPath: String?,
    @ColumnInfo(name = "poster_path")
    val posterPath: String?,
    @ColumnInfo(name = "status")
    val status: String?
)

data class RefreshEpisodeUpdate(
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "show_id")
    val showId: Int,
    @ColumnInfo(name = "tvdb_id")
    val tvdbId: Int?,
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int?,
    @ColumnInfo(name = "imdb_id")
    val imdbId: String?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "language")
    val language: String?,
    @ColumnInfo(name = "overview")
    val overview: String?,
    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int?,
    @ColumnInfo(name = "season_number")
    val seasonNumber: Int?,
    @ColumnInfo(name = "first_aired")
    val firstAired: Long?
)

/**
 * Room DAO for updating a show and reconciling its episode list during refresh.
 *
 * All methods are synchronous and are expected to be called from background threads.
 */
@Dao
interface RefreshShowRoomDao {
    /**
     * Updates core metadata fields for an existing show row.
     *
     * @return number of updated rows.
     */
    @Update(entity = ShowEntity::class)
    fun updateShow(update: RefreshShowUpdate): Int

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
    @Update(entity = EpisodeEntity::class)
    fun updateEpisode(update: RefreshEpisodeUpdate): Int

    /**
     * Inserts a new episode row for the given show.
     *
     * @return inserted row id.
     */
    @Insert
    fun insertEpisode(episode: EpisodeEntity): Long

    /**
     * Deletes one episode by primary key.
     *
     * @return number of deleted rows.
     */
    @Query("DELETE FROM episodes WHERE _id = :episodeId")
    fun deleteEpisodeById(episodeId: Int): Int
}
