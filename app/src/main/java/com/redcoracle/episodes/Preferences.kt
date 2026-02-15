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

package com.redcoracle.episodes

import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object Preferences {
    const val KEY_PREF_THEME_MODE = "pref_theme_mode"
    const val KEY_PREF_ACCENT_COLORS_MODE = "pref_accent_colors_mode"
    const val KEY_PREF_DYNAMIC_COLORS = "pref_dynamic_colors_enabled"
    const val THEME_MODE_SYSTEM = "system"
    const val THEME_MODE_LIGHT = "light"
    const val THEME_MODE_DARK = "dark"
    const val ACCENT_COLORS_APP = "app"
    const val ACCENT_COLORS_DYNAMIC = "dynamic"

    fun getSharedPreferences(): SharedPreferences? {
        val context = MainActivity.getAppContext()
        return context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
    }
}
