package com.redcoracle.keep_track.services

import android.content.Context
import com.redcoracle.keep_track.KeepTrackApplication
import com.redcoracle.keep_track.RefreshShowUtil
import java.util.concurrent.Callable

/**
 * Background task that refreshes one show from TMDB into local storage.
 *
 * Throws when refresh fails so callers using [AsyncTask] can surface a UI error.
 */
class RefreshShowTask(private val showId: Int) : Callable<Void?> {
    private val context: Context = KeepTrackApplication.instance.applicationContext

    /**
     * Performs the refresh and signals failure via exception.
     */
    override fun call(): Void? {
        val refreshed = RefreshShowUtil.refreshShow(showId, context.contentResolver)
        if (!refreshed) {
            throw IllegalStateException("Refresh failed for showId=$showId")
        }
        return null
    }

    companion object {
        private val TAG = RefreshShowTask::class.java.name
    }
}
