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

package com.zarvinx.keep_track

import android.util.Log
import com.zarvinx.keep_track.db.room.AppDatabase
import com.zarvinx.keep_track.db.room.RefreshShowWriter
import com.zarvinx.keep_track.tvdb.Client

/**
 * Helper for refreshing local show metadata/episodes from TMDB.
 */
object RefreshShowUtil {
    private val TAG = RefreshShowUtil::class.java.name

    /**
     * Refreshes one show from remote metadata and writes results locally.
     *
     * @param showId Local show id.
     * @param logFailures Whether caught exceptions should include error logs.
     * @return true when refresh data was fetched and written; false otherwise.
     */
    @JvmStatic
    fun refreshShow(
        showId: Int,
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
                context = KeepTrackApplication.instance.applicationContext
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
        val row = AppDatabase.getInstance(KeepTrackApplication.instance.applicationContext)
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
