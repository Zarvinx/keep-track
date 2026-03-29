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

import java.util.Date

class Episode {
    var id: Int = 0
    var tvdbId: Int? = null
    var tmdbId: Int? = null
    var imdbId: String? = null
    var name: String? = null
    var language: String? = null
    var overview: String? = null
    var episodeNumber: Int = 0
    var seasonNumber: Int = 0
    var firstAired: Date? = null

    fun identifier(): String = "$seasonNumber-$episodeNumber"
}
