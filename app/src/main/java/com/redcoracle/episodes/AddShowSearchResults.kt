/*
 * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
 * Copyright (C) 2026 Zarvinx (Kotlin/Compose conversion)
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

import com.redcoracle.episodes.tvdb.Show

/**
 * Singleton for sharing search results between AddShowSearchActivity and AddShowPreviewActivity.
 *
 * Note: This is a temporary bridge. Replacement is tracked in TODO.md under
 * Architecture Improvements -> State Management.
 */
object AddShowSearchResults {
    var data: List<Show>? = null
}
