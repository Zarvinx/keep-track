/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.redcoracle.episodes.db.room.AppDatabase
import com.redcoracle.episodes.db.room.EpisodeWatchStateWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
data class Episode(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val firstAired: Long?,
    val watched: Boolean
)

class EpisodesViewModel(
    application: Application,
    private val showId: Int,
    private val seasonNumber: Int
) : AndroidViewModel(application) {
    private val appReadDao = AppDatabase.getInstance(application.applicationContext).appReadDao()
    private val watchStateWriter = EpisodeWatchStateWriter(application.applicationContext)
    
    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()
    
    init {
        loadEpisodes()
    }
    
    fun loadEpisodes() {
        viewModelScope.launch {
            val episodesList = withContext(Dispatchers.IO) {
                loadEpisodesFromDatabase()
            }
            _episodes.value = episodesList
        }
    }
    
    private fun loadEpisodesFromDatabase(): List<Episode> {
        return appReadDao.getEpisodesForSeason(showId, seasonNumber).map { row ->
            Episode(
                id = row.id,
                seasonNumber = row.seasonNumber ?: 0,
                episodeNumber = row.episodeNumber ?: 0,
                name = row.name.orEmpty(),
                overview = row.overview,
                firstAired = row.firstAired,
                watched = (row.watched ?: 0) > 0
            )
        }
    }
    
    fun toggleEpisodeWatched(episodeId: Int, watched: Boolean) {
        viewModelScope.launch {
            // Optimistically update UI
            _episodes.value = _episodes.value.map { episode ->
                if (episode.id == episodeId) episode.copy(watched = watched) else episode
            }
            
            // Update database in background
            withContext(Dispatchers.IO) {
                watchStateWriter.setEpisodeWatched(episodeId, watched)
            }
        }
    }
    
    fun markAllWatched(watched: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            watchStateWriter.setSeasonWatched(showId, seasonNumber, watched)
            
            // Reload to reflect changes
            withContext(Dispatchers.Main) {
                loadEpisodes()
            }
        }
    }
}

class EpisodesViewModelFactory(
    private val application: Application,
    private val showId: Int,
    private val seasonNumber: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EpisodesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EpisodesViewModel(application, showId, seasonNumber) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
