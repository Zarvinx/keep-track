package com.zarvinx.keep_track.db.room

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Updates episode watch state through Room.
 *
 * All write operations are suspend functions and should be called from coroutines.
 */
@Singleton
class EpisodeWatchStateWriter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val episodesDao: EpisodesRoomDao =
        AppDatabase.getInstance(context.applicationContext).episodesDao()

    /**
     * Sets watched state for a single episode row.
     */
    suspend fun setEpisodeWatched(episodeId: Int, watched: Boolean) {
        episodesDao.updateEpisodeWatched(episodeId, watched.toDbInt())
    }

    /**
     * Marks or unmarks all episodes in one season.
     *
     * When marking as watched, only episodes that already aired are updated.
     */
    suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean) {
        val nowSeconds = System.currentTimeMillis() / 1000
        if (watched) {
            episodesDao.markAiredSeasonWatched(showId, seasonNumber, nowSeconds)
        } else {
            episodesDao.updateSeasonWatched(showId, seasonNumber, 0)
        }
    }

    /**
     * Marks or unmarks all non-special episodes in a show.
     */
    suspend fun setShowWatched(showId: Int, watched: Boolean) {
        episodesDao.updateShowWatched(showId, watched.toDbInt())
    }

    private fun Boolean.toDbInt(): Int = if (this) 1 else 0
}
