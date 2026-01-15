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

package com.redcoracle.episodes.ui

import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redcoracle.episodes.EpisodesCounter
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Season(
    val seasonNumber: Int,
    val name: String,
    val watchedCount: Int,
    val airedCount: Int,
    val upcomingCount: Int
)

class SeasonsViewModel(application: Application, private val showId: Int) : AndroidViewModel(application) {
    private val contentResolver: ContentResolver = application.contentResolver
    
    private val _seasons = MutableStateFlow<List<Season>>(emptyList())
    val seasons: StateFlow<List<Season>> = _seasons.asStateFlow()
    
    init {
        loadSeasons()
    }
    
    fun loadSeasons() {
        viewModelScope.launch {
            val seasonsList = withContext(Dispatchers.IO) {
                loadSeasonsFromDatabase()
            }
            _seasons.value = seasonsList
        }
    }
    
    private fun loadSeasonsFromDatabase(): List<Season> {
        val seasonsList = mutableListOf<Season>()
        
        val projection = arrayOf(
            EpisodesTable.COLUMN_SEASON_NUMBER,
            EpisodesTable.COLUMN_FIRST_AIRED,
            EpisodesTable.COLUMN_WATCHED
        )
        
        val cursor = contentResolver.query(
            ShowsProvider.CONTENT_URI_EPISODES,
            projection,
            "${EpisodesTable.COLUMN_SHOW_ID}=?",
            arrayOf(showId.toString()),
            "${EpisodesTable.COLUMN_SEASON_NUMBER} ASC"
        )
        
        cursor?.use {
            val episodesCounter = EpisodesCounter(EpisodesTable.COLUMN_SEASON_NUMBER)
            episodesCounter.swapCursor(it)
            
            val seasonNumbers = episodesCounter.keys.toList().sorted()
            
            for (seasonNumber in seasonNumbers) {
                val name = if (seasonNumber == 0) {
                    getApplication<Application>().getString(com.redcoracle.episodes.R.string.season_name_specials)
                } else {
                    getApplication<Application>().getString(com.redcoracle.episodes.R.string.season_name, seasonNumber)
                }
                
                seasonsList.add(
                    Season(
                        seasonNumber = seasonNumber,
                        name = name,
                        watchedCount = episodesCounter.getNumWatchedEpisodes(seasonNumber),
                        airedCount = episodesCounter.getNumAiredEpisodes(seasonNumber),
                        upcomingCount = episodesCounter.getNumUpcomingEpisodes(seasonNumber)
                    )
                )
            }
        }
        
        return seasonsList
    }
}
