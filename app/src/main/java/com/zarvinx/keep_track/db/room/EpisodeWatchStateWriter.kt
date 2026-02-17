package com.zarvinx.keep_track.db.room

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.zarvinx.keep_track.db.ShowsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Updates episode watch state and emits provider notifications for UI observers.
 *
 * All write operations are suspend functions and should be called from coroutines.
 */
@Singleton
class EpisodeWatchStateWriter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val episodesDao: EpisodesRoomDao =
        AppDatabase.getInstance(context.applicationContext).episodesDao()

    /**
     * Sets watched state for a single episode row.
     */
    suspend fun setEpisodeWatched(episodeId: Int, watched: Boolean) {
        episodesDao.updateEpisodeWatched(episodeId, watched.toDbInt())
        notifyEpisodesChanged(episodeId)
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
        notifyEpisodesChanged()
    }

    /**
     * Marks or unmarks all non-special episodes in a show.
     */
    suspend fun setShowWatched(showId: Int, watched: Boolean) {
        episodesDao.updateShowWatched(showId, watched.toDbInt())
        notifyEpisodesChanged()
    }

    private fun notifyEpisodesChanged(episodeId: Int? = null) {
        contentResolver.notifyChange(ShowsProvider.CONTENT_URI_EPISODES, null)
        if (episodeId != null) {
            val episodeUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES, episodeId.toString())
            contentResolver.notifyChange(episodeUri, null)
        }
    }

    private fun Boolean.toDbInt(): Int = if (this) 1 else 0
}
