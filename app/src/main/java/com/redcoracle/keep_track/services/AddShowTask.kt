package com.redcoracle.keep_track.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.redcoracle.keep_track.KeepTrackApplication
import com.redcoracle.keep_track.R
import com.redcoracle.keep_track.db.room.ShowLibraryWriter
import com.redcoracle.keep_track.tvdb.Client
import java.util.concurrent.Callable

/**
 * Background task that fetches and inserts a show selected from add-search flow.
 *
 * Duplicate detection is delegated to [ShowLibraryWriter].
 */
class AddShowTask(
    private val tmdbId: Int,
    private val showName: String,
    private val showLanguage: String
) : Callable<Void?> {
    private val context: Context = KeepTrackApplication.instance.applicationContext
    private val showLibraryWriter = ShowLibraryWriter(context)

    /**
     * Fetches show metadata and inserts it when missing.
     */
    override fun call(): Void? {
        val tmdbClient = Client()
        var show = tmdbClient.getShow(tmdbId, showLanguage, false)

        if (!showLibraryWriter.isAlreadyAdded(show)) {
            showMessage(context.getString(R.string.adding_show, showName))
            show = tmdbClient.getShow(tmdbId, showLanguage, true)
            val added = showLibraryWriter.addShowIfMissing(show)
            if (added) {
                showMessage(context.getString(R.string.show_added, showName))
            } else {
                showMessage(context.getString(R.string.show_already_added, showName))
            }
        } else {
            showMessage(context.getString(R.string.show_already_added, showName))
        }
        return null
    }

    private fun showMessage(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}
