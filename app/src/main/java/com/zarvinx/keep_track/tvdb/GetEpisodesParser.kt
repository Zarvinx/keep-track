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
import com.uwetrottmann.tmdb2.entities.TvEpisode

internal class GetEpisodesParser {
    private val TAG = GetEpisodesParser::class.java.name

    fun parse(tmdbEpisodes: List<TvEpisode>?): List<Episode>? {
        if (tmdbEpisodes == null) return null
        return try {
            tmdbEpisodes.map { episode ->
                Episode().apply {
                    id = episode.id ?: 0
                    tmdbId = episode.id
                    tvdbId = episode.external_ids?.tvdb_id
                    imdbId = episode.external_ids?.imdb_id
                    name = episode.name ?: ""
                    overview = episode.overview
                    seasonNumber = episode.season_number ?: 0
                    episodeNumber = episode.episode_number ?: 0
                    firstAired = episode.air_date
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, e)
            null
        }
    }
}
