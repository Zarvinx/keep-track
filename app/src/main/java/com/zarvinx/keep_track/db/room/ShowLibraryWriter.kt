package com.zarvinx.keep_track.db.room

import android.content.Context
import com.zarvinx.keep_track.tvdb.Show

/**
 * Writes new shows into the local library using Room transactional inserts.
 *
 * This writer is responsible for duplicate detection and transactional inserts.
 */
class ShowLibraryWriter(context: Context) {
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val addShowDao: AddShowRoomDao = roomDb.addShowDao()

    /**
     * Checks whether the incoming show matches an existing row by TMDB, TVDB, or IMDb ID.
     */
    fun isAlreadyAdded(show: Show): Boolean {
        val tvdbId = show.normalizedTvdbId()
        val imdbId = show.normalizedImdbId()
        return addShowDao.findShowIdByTmdbId(show.tmdbId) != null ||
            (tvdbId != null && addShowDao.findShowIdByTvdbId(tvdbId) != null) ||
            (imdbId != null && addShowDao.findShowIdByImdbId(imdbId) != null)
    }

    /**
     * Attempts to insert the show and episodes if no duplicate exists.
     *
     * @return true if a new show row was inserted; false if skipped.
     */
    fun addShowIfMissing(show: Show): Boolean {
        return addShowWithRoom(show)
    }

    private fun addShowWithRoom(show: Show): Boolean {
        val showEntity = show.toShowInsertEntityOrNull() ?: return false
        val tvdbId = show.normalizedTvdbId()
        val imdbId = show.normalizedImdbId()
        val episodes = show.episodes ?: emptyList()

        var added = false
        roomDb.runInTransaction {
            val duplicateFound =
                addShowDao.findShowIdByTmdbId(show.tmdbId) != null ||
                    (tvdbId != null && addShowDao.findShowIdByTvdbId(tvdbId) != null) ||
                    (imdbId != null && addShowDao.findShowIdByImdbId(imdbId) != null)

            if (!duplicateFound) {
                val showId = addShowDao.insertShow(showEntity).toInt()

                episodes.forEach { episode ->
                    addShowDao.insertEpisode(episode.toEpisodeInsertEntity(showId))
                }
                added = true
            }
        }
        return added
    }
}
