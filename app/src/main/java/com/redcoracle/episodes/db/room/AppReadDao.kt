package com.redcoracle.episodes.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification
import kotlinx.coroutines.flow.Flow

data class RefreshShowIdsRow(
    val tvdbId: Int?,
    val tmdbId: Int?,
    val imdbId: String?
)

data class ShowIdNameRow(
    val id: Int,
    val name: String
)

data class EpisodeListRow(
    val id: Int,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val name: String?,
    val overview: String?,
    val firstAired: Long?,
    val watched: Int?
)

data class NextEpisodeRow(
    val id: Int,
    val name: String?,
    val overview: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val firstAired: Long?,
    val watched: Int?
)

data class ShowListRow(
    val id: Int,
    val name: String?,
    val bannerPath: String?,
    val starred: Int?,
    val archived: Int?,
    val status: String?
)

data class EpisodeCountRow(
    val showId: Int?,
    val seasonNumber: Int?,
    val firstAired: Long?,
    val watched: Int?
)

data class SeasonRow(
    val seasonNumber: Int?,
    val firstAired: Long?,
    val watched: Int?
)

data class NextEpisodeInfoRow(
    val id: Int,
    val name: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val firstAired: Long?
)

@Dao
@SkipQueryVerification
interface AppReadDao {
    @Query("SELECT tvdb_id AS tvdbId, tmdb_id AS tmdbId, imdb_id AS imdbId FROM shows WHERE _id = :showId LIMIT 1")
    fun getRefreshShowIds(showId: Int): RefreshShowIdsRow?

    @Query("SELECT _id AS id, name AS name FROM shows ORDER BY name ASC")
    fun getAllShowsForRefresh(): List<ShowIdNameRow>

    @Query(
        "SELECT _id AS id, season_number AS seasonNumber, episode_number AS episodeNumber, " +
            "name AS name, overview AS overview, first_aired AS firstAired, watched AS watched " +
            "FROM episodes WHERE show_id = :showId AND season_number = :seasonNumber ORDER BY episode_number ASC"
    )
    fun getEpisodesForSeason(showId: Int, seasonNumber: Int): List<EpisodeListRow>

    @Query(
        "SELECT _id AS id, name AS name, overview AS overview, season_number AS seasonNumber, " +
            "episode_number AS episodeNumber, first_aired AS firstAired, watched AS watched " +
            "FROM episodes WHERE show_id = :showId AND season_number != 0 AND (watched == 0 OR watched IS NULL) " +
            "ORDER BY season_number ASC, episode_number ASC LIMIT 1"
    )
    fun getNextUnwatchedEpisode(showId: Int): NextEpisodeRow?

    @Query("SELECT _id AS id, name AS name, banner_path AS bannerPath, starred AS starred, archived AS archived, status AS status FROM shows ORDER BY name ASC")
    fun observeShowsForList(): Flow<List<ShowListRow>>

    @Query("SELECT show_id AS showId, season_number AS seasonNumber, first_aired AS firstAired, watched AS watched FROM episodes WHERE season_number != 0")
    fun observeEpisodeCounts(): Flow<List<EpisodeCountRow>>

    @Query("SELECT season_number AS seasonNumber, first_aired AS firstAired, watched AS watched FROM episodes WHERE show_id = :showId ORDER BY season_number ASC, episode_number ASC")
    fun observeSeasonRows(showId: Int): Flow<List<SeasonRow>>

    @Query(
        "SELECT _id AS id, name AS name, season_number AS seasonNumber, episode_number AS episodeNumber, first_aired AS firstAired " +
            "FROM episodes WHERE show_id = :showId AND season_number != 0 AND (watched == 0 OR watched IS NULL) " +
            "ORDER BY season_number ASC, episode_number ASC LIMIT 1"
    )
    fun getNextEpisodeInfo(showId: Int): NextEpisodeInfoRow?
}
