/*
 * Copyright (C) 2014 Jamie Nicol <jamie@thenicols.net>
 * Copyright (C) 2026 Zarvinx (Kotlin conversion)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redcoracle.episodes

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
import com.redcoracle.episodes.db.room.RefreshShowWriter
import com.redcoracle.episodes.tvdb.Client

object RefreshShowUtil {
    private val TAG = RefreshShowUtil::class.java.name

    @JvmStatic
    fun refreshShow(showId: Int, contentResolver: ContentResolver) {
        Log.i(TAG, "Refreshing show $showId")

        val tmdbClient = Client()
        val preferences = Preferences.getSharedPreferences()

        val showLanguage = preferences?.getString("pref_language", "en") ?: "en"
        val showIds = getShowIds(showId, contentResolver)
        val show = tmdbClient.getShow(showIds, showLanguage)

        if (show != null) {
            show.episodes?.let { episodes ->
                val roomEpisodes = episodes.toMutableList()
                val writer = RefreshShowWriter(
                    context = EpisodesApplication.instance.applicationContext,
                    contentResolver = contentResolver
                )
                writer.refreshShow(
                    showId = showId,
                    show = show,
                    episodes = roomEpisodes
                )
            }
        }
    }

    private fun getShowIds(showId: Int, contentResolver: ContentResolver): HashMap<String, String> {
        val showUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, showId.toString())
        val projection = arrayOf(
            ShowsTable.COLUMN_TVDB_ID,
            ShowsTable.COLUMN_TMDB_ID,
            ShowsTable.COLUMN_IMDB_ID
        )
        
        val showIds = HashMap<String, String>()
        contentResolver.query(showUri, projection, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            val tvdbIdColumnIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_TVDB_ID)
            val tmdbIdColumnIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_TMDB_ID)
            val imdbIdColumnIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_IMDB_ID)
            
            showIds["tvdbId"] = cursor.getString(tvdbIdColumnIndex)
            showIds["tmdbId"] = cursor.getString(tmdbIdColumnIndex)
            showIds["imdbId"] = cursor.getString(imdbIdColumnIndex)
        }
        
        return showIds
    }

}
