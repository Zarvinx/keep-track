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
    const val KEY_PREF_BACKGROUND_GRADIENT = "pref_background_gradient"
    const val KEY_PREF_DYNAMIC_COLORS = "pref_dynamic_colors_enabled"
    const val THEME_MODE_SYSTEM = "system"
    const val THEME_MODE_LIGHT = "light"
    const val THEME_MODE_DARK = "dark"
    const val ACCENT_COLORS_APP = "app"
    const val ACCENT_COLORS_DYNAMIC = "dynamic"
    const val ACCENT_COLORS_CUSTOM_PREFIX = "custom:"
    const val BACKGROUND_GRADIENT_MIST_BLUE = "mist_blue"
    const val BACKGROUND_GRADIENT_SUNSET_AMBER = "sunset_amber"
    const val BACKGROUND_GRADIENT_CITRUS_LIME = "citrus_lime"
    const val BACKGROUND_GRADIENT_DEEP_FOREST = "deep_forest"
    const val BACKGROUND_GRADIENT_AQUA_SKY = "aqua_sky"
    const val BACKGROUND_GRADIENT_ROYAL_PLUM = "royal_plum"
    const val BACKGROUND_GRADIENT_NIGHT_BLUE = "night_blue"
    const val BACKGROUND_GRADIENT_DEEP_OCEAN = "deep_ocean"
    const val BACKGROUND_GRADIENT_NIGHT_MINT = "night_mint"
    const val BACKGROUND_GRADIENT_PINE_SHADOW = "pine_shadow"
    const val BACKGROUND_GRADIENT_BURNT_AMBER = "burnt_amber"
    const val BACKGROUND_GRADIENT_VIOLET_NIGHT = "violet_night"
    const val BACKGROUND_GRADIENT_CRIMSON_NIGHT = "crimson_night"

    fun getSharedPreferences(): SharedPreferences? {
        val context = MainActivity.getAppContext()
        return context?.let { PreferenceManager.getDefaultSharedPreferences(it) }
    }
}
