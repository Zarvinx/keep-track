package com.redcoracle.episodes.services

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.R
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
import com.redcoracle.episodes.tvdb.Client
import com.redcoracle.episodes.tvdb.Episode
import com.redcoracle.episodes.tvdb.Show
import java.util.concurrent.Callable

class AddShowTask(
    private val tmdbId: Int,
    private val showName: String,
    private val showLanguage: String
) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext

    override fun call(): Void? {
        val tmdbClient = Client()
        var show = tmdbClient.getShow(tmdbId, showLanguage, false)

        if (!checkAlreadyAdded(show)) {
            showMessage(context.getString(R.string.adding_show, showName))
            show = tmdbClient.getShow(tmdbId, showLanguage, true)
            val showId = insertShow(show)
            insertEpisodes(show.episodes.toTypedArray(), showId)
            showMessage(context.getString(R.string.show_added, showName))
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

    private fun insertShow(show: Show): Int {
        val showValues = ContentValues().apply {
            if (show.tvdbId != 0) {
                put(ShowsTable.COLUMN_TVDB_ID, show.tvdbId)
            }
            put(ShowsTable.COLUMN_TMDB_ID, show.tmdbId)
            put(ShowsTable.COLUMN_IMDB_ID, show.imdbId)
            put(ShowsTable.COLUMN_NAME, show.name)
            put(ShowsTable.COLUMN_LANGUAGE, show.language)
            put(ShowsTable.COLUMN_OVERVIEW, show.overview)
            show.firstAired?.let {
                put(ShowsTable.COLUMN_FIRST_AIRED, it.time / 1000)
            }
            put(ShowsTable.COLUMN_BANNER_PATH, show.bannerPath)
            put(ShowsTable.COLUMN_FANART_PATH, show.fanartPath)
            put(ShowsTable.COLUMN_POSTER_PATH, show.posterPath)
        }

        val showUri: Uri = context.contentResolver.insert(ShowsProvider.CONTENT_URI_SHOWS, showValues)
            ?: throw IllegalStateException("Failed to insert show")
        val showId = showUri.lastPathSegment?.toInt()
            ?: throw IllegalStateException("Failed to get show ID")
        Log.i(TAG, "show ${show.name} successfully added to database as row $showId. adding episodes")
        return showId
    }

    private fun insertEpisodes(episodes: Array<Episode>, showId: Int) {
        val values = episodes.map { episode ->
            ContentValues().apply {
                put(EpisodesTable.COLUMN_TVDB_ID, episode.tvdbId)
                put(EpisodesTable.COLUMN_TMDB_ID, episode.tmdbId)
                put(EpisodesTable.COLUMN_IMDB_ID, episode.imdbId)
                put(EpisodesTable.COLUMN_SHOW_ID, showId)
                put(EpisodesTable.COLUMN_NAME, episode.name)
                put(EpisodesTable.COLUMN_LANGUAGE, episode.language)
                put(EpisodesTable.COLUMN_OVERVIEW, episode.overview)
                put(EpisodesTable.COLUMN_EPISODE_NUMBER, episode.episodeNumber)
                put(EpisodesTable.COLUMN_SEASON_NUMBER, episode.seasonNumber)
                episode.firstAired?.let {
                    put(EpisodesTable.COLUMN_FIRST_AIRED, it.time / 1000)
                }
            }
        }

        values.forEach { value ->
            context.contentResolver.insert(ShowsProvider.CONTENT_URI_EPISODES, value)
        }
    }

    private fun showMessage(message: String) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "AddShowTask"
    }
}
