/*
 * Copyright (C) 2012-2015 Jamie Nicol <jamie@thenicols.net>
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

package com.redcoracle.keep_track.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcoracle.keep_track.R
import com.redcoracle.keep_track.db.room.AppReadDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class Season(
    val seasonNumber: Int,
    val name: String,
    val watchedCount: Int,
    val airedCount: Int,
    val upcomingCount: Int
)

@HiltViewModel
class SeasonsViewModel @Inject constructor(
    private val appReadDao: AppReadDao,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private var showId: Int? = null
    
    private val _seasons = MutableStateFlow<List<Season>>(emptyList())
    val seasons: StateFlow<List<Season>> = _seasons.asStateFlow()
    
    fun initialize(showId: Int) {
        if (this.showId == showId) return
        this.showId = showId
        viewModelScope.launch {
            appReadDao.observeSeasonRows(showId).collectLatest { rows ->
                val seasonsList = withContext(Dispatchers.Default) {
                    loadSeasonsFromRows(rows)
                }
                _seasons.value = seasonsList
            }
        }
    }
    
    private fun loadSeasonsFromRows(rows: List<com.redcoracle.keep_track.db.room.SeasonRow>): List<Season> {
        val nowSeconds = System.currentTimeMillis() / 1000
        val grouped = rows.groupBy { it.seasonNumber ?: 0 }
        return grouped.keys.sorted().map { seasonNumber ->
            val seasonRows = grouped[seasonNumber].orEmpty()
            var watchedCount = 0
            var airedCount = 0
            var upcomingCount = 0
            for (row in seasonRows) {
                if ((row.watched ?: 0) > 0) {
                    watchedCount += 1
                }
                val firstAired = row.firstAired ?: 0L
                if (firstAired > 0) {
                    if (firstAired <= nowSeconds) {
                        airedCount += 1
                    } else {
                        upcomingCount += 1
                    }
                }
            }

            val name = if (seasonNumber == 0) {
                context.getString(R.string.season_name_specials)
            } else {
                context.getString(R.string.season_name, seasonNumber)
            }

            Season(
                seasonNumber = seasonNumber,
                name = name,
                watchedCount = watchedCount,
                airedCount = airedCount,
                upcomingCount = upcomingCount
            )
        }
    }
}
