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
import android.util.Log
import com.redcoracle.episodes.db.room.AppDatabase
import com.redcoracle.episodes.db.room.RefreshShowWriter
import com.redcoracle.episodes.tvdb.Client

object RefreshShowUtil {
    private val TAG = RefreshShowUtil::class.java.name

    @JvmStatic
    fun refreshShow(
        showId: Int,
        contentResolver: ContentResolver,
        logFailures: Boolean = true
    ): Boolean {
        return try {
            Log.i(TAG, "Refreshing show $showId")

            val tmdbClient = Client()
            val preferences = Preferences.getSharedPreferences()

            val showLanguage = preferences?.getString("pref_language", "en") ?: "en"
            val showIds = getShowIds(showId)
            val show = tmdbClient.getShow(showIds, showLanguage)

            if (show == null) {
                Log.w(TAG, "Skipping refresh for showId=$showId because no show details were returned.")
                return false
            }

            val episodes = show.episodes
            if (episodes == null) {
                Log.w(TAG, "Skipping refresh for showId=$showId because no episode list was returned.")
                return false
            }

            val writer = RefreshShowWriter(
                context = EpisodesApplication.instance.applicationContext,
                contentResolver = contentResolver
            )
            writer.refreshShow(
                showId = showId,
                show = show,
                episodes = episodes.toMutableList()
            )
            true
        } catch (e: Exception) {
            if (logFailures) {
                Log.e(TAG, "Failed to refresh showId=$showId", e)
            }
            false
        }
    }

    private fun getShowIds(showId: Int): HashMap<String, String> {
        val showIds = HashMap<String, String>()
        val row = AppDatabase.getInstance(EpisodesApplication.instance.applicationContext)
            .appReadDao()
            .getRefreshShowIds(showId)

        row?.tvdbId?.toString()?.let { showIds["tvdbId"] = it }
        row?.tmdbId?.toString()?.let { showIds["tmdbId"] = it }
        row?.imdbId?.let { showIds["imdbId"] = it }

        if (showIds.isEmpty()) {
            Log.w(TAG, "No show IDs found for showId=$showId")
        }
        return showIds
    }

}
