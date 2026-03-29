/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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
import com.uwetrottmann.tmdb2.entities.TvShow

internal class GetShowParser {
    private val TAG = "GetShowParser"

    fun parse(series: TvShow, language: String): Show? {
        return try {
            val id = series.id ?: run {
                Log.w(TAG, "Show does not have an ID: ${series.name}")
                return null
            }
            Show().apply {
                this.id = id
                tmdbId = id
                tvdbId = series.external_ids?.tvdb_id ?: 0
                imdbId = series.external_ids?.imdb_id
                name = series.name
                this.language = language
                overview = series.overview
                firstAired = series.first_air_date
                bannerPath = series.backdrop_path
                posterPath = series.poster_path
                status = series.status
                seasonNames = series.seasons
                    ?.filter { season -> season.season_number != null && !season.name.isNullOrEmpty() }
                    ?.associate { season -> season.season_number!! to season.name!! }
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }
}
