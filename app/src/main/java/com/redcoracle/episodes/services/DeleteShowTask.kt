package com.redcoracle.episodes.services

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import java.util.concurrent.Callable

class DeleteShowTask(private val showId: Int) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext

    override fun call(): Void? {
        val resolver: ContentResolver = context.contentResolver
        val selection = "${EpisodesTable.COLUMN_SHOW_ID}=?"
        val selectionArgs = arrayOf(showId.toString())
        val episodes = resolver.delete(ShowsProvider.CONTENT_URI_EPISODES, selection, selectionArgs)
        Log.d(TAG, "Deleted $episodes episodes")
        resolver.delete(Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, showId.toString()), null, null)
        return null
    }

    companion object {
        private val TAG = DeleteShowTask::class.java.name
    }
}
