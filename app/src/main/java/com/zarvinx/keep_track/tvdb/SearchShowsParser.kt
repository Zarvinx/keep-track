/*
 * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
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

package com.zarvinx.keep_track.tvdb

import android.util.Log
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage

internal class SearchShowsParser {
    private val TAG = SearchShowsParser::class.java.name

    fun parse(results: TvShowResultsPage, language: String): List<Show> {
        return try {
            results.results?.map { s ->
                Show().apply {
                    id = s.id ?: 0
                    tmdbId = s.id ?: 0
                    name = s.name
                    this.language = language
                    overview = s.overview
                    firstAired = s.first_air_date
                    posterPath = s.poster_path
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, e)
            emptyList()
        }
    }
}
