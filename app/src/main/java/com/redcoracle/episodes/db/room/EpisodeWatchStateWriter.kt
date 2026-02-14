package com.redcoracle.episodes.db.room

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.redcoracle.episodes.db.ShowsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeWatchStateWriter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val episodesDao: EpisodesRoomDao =
        AppDatabase.getInstance(context.applicationContext).episodesDao()

    suspend fun setEpisodeWatched(episodeId: Int, watched: Boolean) {
        episodesDao.updateEpisodeWatched(episodeId, watched.toDbInt())
        notifyEpisodesChanged(episodeId)
    }

    suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean) {
        val nowSeconds = System.currentTimeMillis() / 1000
        if (watched) {
            episodesDao.markAiredSeasonWatched(showId, seasonNumber, nowSeconds)
        } else {
            episodesDao.updateSeasonWatched(showId, seasonNumber, 0)
        }
        notifyEpisodesChanged()
    }

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
