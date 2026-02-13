package com.redcoracle.episodes.db.room

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
import com.redcoracle.episodes.tvdb.Show

/**
 * Transitional writer for add-show operations during incremental Room migration.
 *
 * This writer attempts a Room transaction first (show + episodes), then falls back to
 * legacy ContentResolver inserts if Room cannot open due schema-validation mismatch.
 */
class ShowLibraryWriter(context: Context) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val addShowDao: AddShowRoomDao = roomDb.addShowDao()

    fun addShowIfMissing(show: Show): Boolean {
        val added = runCatching {
            addShowWithRoom(show)
        }.getOrElse {
            addShowWithLegacyResolver(show)
        }

        if (added) {
            contentResolver.notifyChange(ShowsProvider.CONTENT_URI_SHOWS, null)
            contentResolver.notifyChange(ShowsProvider.CONTENT_URI_EPISODES, null)
        }
        return added
    }

    private fun addShowWithRoom(show: Show): Boolean {
        val tvdbId = show.tvdbId.takeIf { it > 0 }
        val imdbId = show.imdbId?.takeIf { it.isNotBlank() }

        var added = false
        roomDb.runInTransaction {
            val duplicateFound =
                addShowDao.findShowIdByTmdbId(show.tmdbId) != null ||
                    (tvdbId != null && addShowDao.findShowIdByTvdbId(tvdbId) != null) ||
                    (imdbId != null && addShowDao.findShowIdByImdbId(imdbId) != null)

            if (!duplicateFound) {
                val showId = addShowDao.insertShow(
                    tvdbId = tvdbId,
                    tmdbId = show.tmdbId,
                    imdbId = imdbId,
                    name = show.name,
                    language = show.language,
                    overview = show.overview,
                    firstAired = show.firstAired?.time?.div(1000),
                    bannerPath = show.bannerPath,
                    fanartPath = show.fanartPath,
                    posterPath = show.posterPath
                ).toInt()

                show.episodes.forEach { episode ->
                    addShowDao.insertEpisode(
                        tvdbId = episode.tvdbId.takeIf { it > 0 },
                        tmdbId = episode.tmdbId.takeIf { it > 0 },
                        imdbId = episode.imdbId?.takeIf { it.isNotBlank() },
                        showId = showId,
                        name = episode.name,
                        language = episode.language,
                        overview = episode.overview,
                        episodeNumber = episode.episodeNumber,
                        seasonNumber = episode.seasonNumber,
                        firstAired = episode.firstAired?.time?.div(1000)
                    )
                }
                added = true
            }
        }
        return added
    }

    private fun addShowWithLegacyResolver(show: Show): Boolean {
        val showValues = ContentValues().apply {
            if (show.tvdbId > 0) {
                put(ShowsTable.COLUMN_TVDB_ID, show.tvdbId)
            }
            put(ShowsTable.COLUMN_TMDB_ID, show.tmdbId)
            put(ShowsTable.COLUMN_IMDB_ID, show.imdbId)
            put(ShowsTable.COLUMN_NAME, show.name)
            put(ShowsTable.COLUMN_LANGUAGE, show.language)
            put(ShowsTable.COLUMN_OVERVIEW, show.overview)
            show.firstAired?.let {
                put(ShowsTable.COLUMN_FIRST_AIRED, it.time / 1000)
            }
            put(ShowsTable.COLUMN_BANNER_PATH, show.bannerPath)
            put(ShowsTable.COLUMN_FANART_PATH, show.fanartPath)
            put(ShowsTable.COLUMN_POSTER_PATH, show.posterPath)
        }

        val showUri: Uri = contentResolver.insert(ShowsProvider.CONTENT_URI_SHOWS, showValues) ?: return false
        val showId = showUri.lastPathSegment?.toIntOrNull() ?: return false

        show.episodes.forEach { episode ->
            val values = ContentValues().apply {
                put(EpisodesTable.COLUMN_TVDB_ID, episode.tvdbId)
                put(EpisodesTable.COLUMN_TMDB_ID, episode.tmdbId)
                put(EpisodesTable.COLUMN_IMDB_ID, episode.imdbId)
                put(EpisodesTable.COLUMN_SHOW_ID, showId)
                put(EpisodesTable.COLUMN_NAME, episode.name)
                put(EpisodesTable.COLUMN_LANGUAGE, episode.language)
                put(EpisodesTable.COLUMN_OVERVIEW, episode.overview)
                put(EpisodesTable.COLUMN_EPISODE_NUMBER, episode.episodeNumber)
                put(EpisodesTable.COLUMN_SEASON_NUMBER, episode.seasonNumber)
                episode.firstAired?.let {
                    put(EpisodesTable.COLUMN_FIRST_AIRED, it.time / 1000)
                }
            }
            contentResolver.insert(ShowsProvider.CONTENT_URI_EPISODES, values)
        }
        return true
    }
}
