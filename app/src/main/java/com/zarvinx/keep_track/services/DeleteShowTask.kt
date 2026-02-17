package com.zarvinx.keep_track.services

import android.content.Context
import android.util.Log
import com.zarvinx.keep_track.KeepTrackApplication
import com.zarvinx.keep_track.db.room.ShowMutationsWriter
import java.util.concurrent.Callable

/**
 * Background task that deletes a show and all its episodes.
 */
class DeleteShowTask(private val showId: Int) : Callable<Void?> {
    private val context: Context = KeepTrackApplication.instance.applicationContext
    private val showMutationsWriter = ShowMutationsWriter(context)

    /**
     * Executes the delete operation and logs deleted episode count.
     */
    override fun call(): Void? {
        val episodes = showMutationsWriter.deleteShow(showId)
        Log.d(TAG, "Deleted $episodes episodes")
        return null
    }

    companion object {
        private val TAG = DeleteShowTask::class.java.name
    }
}
