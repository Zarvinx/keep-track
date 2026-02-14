package com.redcoracle.episodes.db.room

import android.content.ContentResolver
import android.content.Context
import com.redcoracle.episodes.db.ShowsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowMutationsWriter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val dao: ShowMutationsDao = roomDb.showMutationsDao()

    fun setStarred(showId: Int, starred: Boolean) {
        dao.updateStarred(showId, if (starred) 1 else 0)
        notifyShowsChanged()
    }

    fun setArchived(showId: Int, archived: Boolean) {
        dao.updateArchived(showId, if (archived) 1 else 0)
        notifyShowsChanged()
    }

    fun deleteShow(showId: Int): Int {
        val deletedEpisodesAndShow = roomDb.runInTransaction<Int> {
            val episodes = dao.deleteEpisodesByShowId(showId)
            dao.deleteShowById(showId)
            episodes
        }
        notifyShowsAndEpisodesChanged()
        return deletedEpisodesAndShow
    }

    private fun notifyShowsChanged() {
        contentResolver.notifyChange(ShowsProvider.CONTENT_URI_SHOWS, null)
    }

    private fun notifyShowsAndEpisodesChanged() {
        contentResolver.notifyChange(ShowsProvider.CONTENT_URI_SHOWS, null)
        contentResolver.notifyChange(ShowsProvider.CONTENT_URI_EPISODES, null)
    }
}
