package com.zarvinx.keep_track.db.room

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates show-level mutation writes (star/archive/delete).
 */
@Singleton
class ShowMutationsWriter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val dao: ShowMutationsDao = roomDb.showMutationsDao()

    /**
     * Updates the starred flag for one show.
     */
    fun setStarred(showId: Int, starred: Boolean) {
        dao.updateStarred(showId, if (starred) 1 else 0)
    }

    /**
     * Updates the archived flag for one show.
     */
    fun setArchived(showId: Int, archived: Boolean) {
        dao.updateArchived(showId, if (archived) 1 else 0)
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
        return deletedEpisodesAndShow
    }
}
