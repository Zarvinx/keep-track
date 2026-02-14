package com.redcoracle.episodes.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.R
import com.redcoracle.episodes.db.room.ShowLibraryWriter
import com.redcoracle.episodes.tvdb.Client
import java.util.concurrent.Callable

class AddShowTask(
    private val tmdbId: Int,
    private val showName: String,
    private val showLanguage: String
) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext
    private val showLibraryWriter = ShowLibraryWriter(context)

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
