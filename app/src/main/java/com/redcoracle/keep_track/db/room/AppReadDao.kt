package com.redcoracle.keep_track.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification
import kotlinx.coroutines.flow.Flow

/**
 * Identifier set used when resolving a show against TMDB and legacy providers.
 */
data class RefreshShowIdsRow(
    val tvdbId: Int?,
    val tmdbId: Int?,
    val imdbId: String?
)

/**
 * Minimal projection used by "refresh all" background tasks.
 */
data class ShowIdNameRow(
    val id: Int,
    val name: String
)

/**
 * Episode row projection for per-season lists.
 */
data class EpisodeListRow(
    val id: Int,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val name: String?,
    val overview: String?,
    val firstAired: Long?,
    val watched: Int?
)

/**
 * Projection for the next unwatched episode in a show.
 */
data class NextEpisodeRow(
    val id: Int,
    val name: String?,
    val overview: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val firstAired: Long?,
    val watched: Int?
)

/**
 * Projection used by show list UI rendering.
 */
data class ShowListRow(
    val id: Int,
    val name: String?,
    val bannerPath: String?,
    val starred: Int?,
    val archived: Int?,
    val status: String?
)

/**
 * Episode count projection used to derive watched/aired/upcoming totals.
 */
data class EpisodeCountRow(
    val showId: Int?,
    val seasonNumber: Int?,
    val firstAired: Long?,
    val watched: Int?
)

/**
 * Projection used to aggregate season-level progress.
 */
data class SeasonRow(
    val seasonNumber: Int?,
    val firstAired: Long?,
    val watched: Int?
)

/**
 * Projection for the next episode summary displayed in show cards.
 */
data class NextEpisodeInfoRow(
    val id: Int,
    val name: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val firstAired: Long?
)

/**
 * Read-only Room DAO that powers UI state derivation and refresh lookups.
 *
 * Query methods are synchronous unless they return [Flow], and callers should
 * run synchronous calls off the main thread.
 */
@Dao
@SkipQueryVerification
interface AppReadDao {
    /**
     * Returns external IDs for a show, or null if the show does not exist.
     */
    @Query("SELECT tvdb_id AS tvdbId, tmdb_id AS tmdbId, imdb_id AS imdbId FROM shows WHERE _id = :showId LIMIT 1")
    fun getRefreshShowIds(showId: Int): RefreshShowIdsRow?

    /**
     * Returns all shows sorted by name for bulk refresh operations.
     */
    @Query("SELECT _id AS id, name AS name FROM shows ORDER BY name ASC")
    fun getAllShowsForRefresh(): List<ShowIdNameRow>

    /**
     * Returns episodes for one season ordered by episode number.
     */
    @Query(
        "SELECT _id AS id, season_number AS seasonNumber, episode_number AS episodeNumber, " +
            "name AS name, overview AS overview, first_aired AS firstAired, watched AS watched " +
            "FROM episodes WHERE show_id = :showId AND season_number = :seasonNumber ORDER BY episode_number ASC"
    )
    fun getEpisodesForSeason(showId: Int, seasonNumber: Int): List<EpisodeListRow>

    /**
     * Returns the earliest unwatched non-special episode for a show.
     */
    @Query(
        "SELECT _id AS id, name AS name, overview AS overview, season_number AS seasonNumber, " +
            "episode_number AS episodeNumber, first_aired AS firstAired, watched AS watched " +
            "FROM episodes WHERE show_id = :showId AND season_number != 0 AND (watched == 0 OR watched IS NULL) " +
            "ORDER BY season_number ASC, episode_number ASC LIMIT 1"
    )
    fun getNextUnwatchedEpisode(showId: Int): NextEpisodeRow?

    /**
     * Emits show list projections whenever the underlying show rows change.
     */
    @Query("SELECT _id AS id, name AS name, banner_path AS bannerPath, starred AS starred, archived AS archived, status AS status FROM shows ORDER BY name ASC")
    fun observeShowsForList(): Flow<List<ShowListRow>>

    /**
     * Emits episode rows required to compute progress totals.
     */
    @Query("SELECT show_id AS showId, season_number AS seasonNumber, first_aired AS firstAired, watched AS watched FROM episodes WHERE season_number != 0")
    fun observeEpisodeCounts(): Flow<List<EpisodeCountRow>>

    /**
     * Emits raw per-episode rows for deriving grouped season state.
     */
    @Query("SELECT season_number AS seasonNumber, first_aired AS firstAired, watched AS watched FROM episodes WHERE show_id = :showId ORDER BY season_number ASC, episode_number ASC")
    fun observeSeasonRows(showId: Int): Flow<List<SeasonRow>>

    /**
     * Returns a compact next-episode summary for show cards.
     */
    @Query(
        "SELECT _id AS id, name AS name, season_number AS seasonNumber, episode_number AS episodeNumber, first_aired AS firstAired " +
            "FROM episodes WHERE show_id = :showId AND season_number != 0 AND (watched == 0 OR watched IS NULL) " +
            "ORDER BY season_number ASC, episode_number ASC LIMIT 1"
    )
    fun getNextEpisodeInfo(showId: Int): NextEpisodeInfoRow?
}
