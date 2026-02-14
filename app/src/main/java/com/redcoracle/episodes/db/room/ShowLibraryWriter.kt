package com.redcoracle.episodes.db.room

import android.content.ContentResolver
import android.content.Context
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.tvdb.Show

class ShowLibraryWriter(context: Context) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val addShowDao: AddShowRoomDao = roomDb.addShowDao()

    fun addShowIfMissing(show: Show): Boolean {
        val added = addShowWithRoom(show)
        if (added) {
            contentResolver.notifyChange(ShowsProvider.CONTENT_URI_SHOWS, null)
            contentResolver.notifyChange(ShowsProvider.CONTENT_URI_EPISODES, null)
        }
        return added
    }

    private fun addShowWithRoom(show: Show): Boolean {
        val showName = show.name ?: return false
        val tvdbId = show.tvdbId.takeIf { it > 0 }
        val imdbId = show.imdbId?.takeIf { it.isNotBlank() }
        val episodes = show.episodes ?: emptyList()

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
                    name = showName,
                    language = show.language,
                    overview = show.overview,
                    firstAired = show.firstAired?.time?.div(1000),
                    bannerPath = show.bannerPath,
                    fanartPath = show.fanartPath,
                    posterPath = show.posterPath
                ).toInt()

                episodes.forEach { episode ->
                    addShowDao.insertEpisode(
                        tvdbId = episode.tvdbId?.takeIf { it > 0 },
                        tmdbId = episode.tmdbId?.takeIf { it > 0 },
                        imdbId = episode.imdbId?.takeIf { it.isNotBlank() },
                        showId = showId,
                        name = episode.name ?: "",
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
}
