package com.redcoracle.keep_track.db.room

import android.content.ContentResolver
import android.content.Context
import com.redcoracle.keep_track.db.ShowsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates show-level mutation writes (star/archive/delete) plus change notifications.
 */
@Singleton
class ShowMutationsWriter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val dao: ShowMutationsDao = roomDb.showMutationsDao()

    /**
     * Updates the starred flag for one show.
     */
    fun setStarred(showId: Int, starred: Boolean) {
        dao.updateStarred(showId, if (starred) 1 else 0)
        notifyShowsChanged()
    }

    /**
     * Updates the archived flag for one show.
     */
    fun setArchived(showId: Int, archived: Boolean) {
        dao.updateArchived(showId, if (archived) 1 else 0)
        notifyShowsChanged()
    }

    /**
     * Deletes a show and its episodes in one transaction.
     *
     * @return number of episode rows removed.
     */
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
