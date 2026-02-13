package com.redcoracle.episodes.db.room

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.redcoracle.episodes.db.ShowsProvider

class EpisodeWatchStateWriter(context: Context) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val episodesDao: EpisodesRoomDao =
        EpisodesRoomDatabase.getInstance(context.applicationContext).episodesDao()

    suspend fun setEpisodeWatched(episodeId: Int, watched: Boolean) {
        episodesDao.updateEpisodeWatched(episodeId, watched.toDbInt())
        notifyEpisodesChanged(episodeId)
    }

    suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean) {
        if (watched) {
            val nowSeconds = System.currentTimeMillis() / 1000
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
