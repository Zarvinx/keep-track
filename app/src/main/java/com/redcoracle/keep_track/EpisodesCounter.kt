/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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

package com.redcoracle.keep_track

import android.database.Cursor
import android.util.SparseIntArray
import com.redcoracle.keep_track.db.EpisodesTable
import java.util.*

class EpisodesCounter(private val keyColumn: String) {
    private val keys: MutableSet<Int>
    private val numAiredEpisodesMap = SparseIntArray()
    private val numWatchedEpisodesMap = SparseIntArray()
    private val numUpcomingEpisodesMap = SparseIntArray()

    init {
        keys = TreeSet()
    }

    fun swapCursor(episodesCursor: Cursor?) {
        keys.clear()
        numAiredEpisodesMap.clear()
        numWatchedEpisodesMap.clear()
        numUpcomingEpisodesMap.clear()

        if (episodesCursor == null || !episodesCursor.moveToFirst()) {
            return
        }

        do {
            val keyColumnIndex = episodesCursor.getColumnIndexOrThrow(keyColumn)
            val key = episodesCursor.getInt(keyColumnIndex)

            // Check if episode is aired, watched, or upcoming
            val seasonNumberColumnIndex = episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER)
            val seasonNumber = episodesCursor.getInt(seasonNumberColumnIndex)

            val firstAiredColumnIndex = episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_FIRST_AIRED)
            val firstAired = if (!episodesCursor.isNull(firstAiredColumnIndex)) {
                Date(episodesCursor.getLong(firstAiredColumnIndex) * 1000)
            } else {
                null
            }

            val watchedColumnIndex = episodesCursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED)
            val watched = episodesCursor.getInt(watchedColumnIndex) > 0

            keys.add(key)

            // Increment the appropriate counter(s) for this show.
            // Count shows with no aired date as upcoming,
            // unless they're specials in which case count them as aired.
            if ((firstAired != null && firstAired.before(Date())) || seasonNumber == 0) {
                numAiredEpisodesMap.put(key, numAiredEpisodesMap.get(key) + 1)
                if (watched) {
                    numWatchedEpisodesMap.put(key, numWatchedEpisodesMap.get(key) + 1)
                }
            } else {
                numUpcomingEpisodesMap.put(key, numUpcomingEpisodesMap.get(key) + 1)
            }
        } while (episodesCursor.moveToNext())
    }

    fun getKeys(): Set<Int> = keys

    fun getNumAiredEpisodes(key: Int): Int {
        return numAiredEpisodesMap.get(key, 0)
    }

    fun getNumWatchedEpisodes(key: Int): Int {
        return numWatchedEpisodesMap.get(key, 0)
    }

    fun getNumUpcomingEpisodes(key: Int): Int {
        return numUpcomingEpisodesMap.get(key, 0)
    }
}
