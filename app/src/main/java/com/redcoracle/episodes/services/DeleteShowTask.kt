package com.redcoracle.episodes.services

import android.content.Context
import android.util.Log
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.db.room.ShowMutationsWriter
import java.util.concurrent.Callable

class DeleteShowTask(private val showId: Int) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext
    private val showMutationsWriter = ShowMutationsWriter(context)

    override fun call(): Void? {
        val episodes = showMutationsWriter.deleteShow(showId)
        Log.d(TAG, "Deleted $episodes episodes")
        return null
    }

    companion object {
        private val TAG = DeleteShowTask::class.java.name
    }
}
