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

import java.util.Date

class Show {
    var id: Int = 0
    var tvdbId: Int = 0
    var tmdbId: Int = 0
    var imdbId: String? = null
    var name: String? = null
    var language: String = ""
    var overview: String? = null
    var firstAired: Date? = null
    var bannerPath: String? = null
    var fanartPath: String? = null
    var posterPath: String? = null
    var status: String? = null
    var episodes: List<Episode>? = null
    var seasonNames: Map<Int, String>? = null
}
