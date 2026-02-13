package com.redcoracle.episodes.services

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.R
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
import com.redcoracle.episodes.db.room.ShowLibraryWriter
import com.redcoracle.episodes.tvdb.Client
import com.redcoracle.episodes.tvdb.Show
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

        if (!checkAlreadyAdded(show)) {
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

    private fun checkAlreadyAdded(show: Show): Boolean {
        val projection = arrayOf<String>()
        var selection = "${ShowsTable.COLUMN_TMDB_ID}=?"
        val selectionArgs = mutableListOf<String>()
        selectionArgs.add(show.tmdbId.toString())

        if (show.tvdbId > 0) {
            selection += " OR ${ShowsTable.COLUMN_TVDB_ID}=?"
            selectionArgs.add(show.tvdbId.toString())
        }
        if (show.imdbId != null && show.imdbId.isNotEmpty()) {
            selection += " OR ${ShowsTable.COLUMN_IMDB_ID}=?"
            selectionArgs.add(show.imdbId)
        }

        val resolver: ContentResolver = context.contentResolver
        val cursor: Cursor = resolver.query(
            ShowsProvider.CONTENT_URI_SHOWS,
            projection,
            selection,
            selectionArgs.toTypedArray(),
            null
        ) ?: return false

        val existing = cursor.moveToFirst()
        cursor.close()
        return existing
    }

    private fun showMessage(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}
