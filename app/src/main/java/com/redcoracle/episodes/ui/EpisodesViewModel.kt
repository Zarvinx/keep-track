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

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcoracle.episodes.db.room.AppReadDao
import com.redcoracle.episodes.db.room.EpisodeWatchStateWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

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

@HiltViewModel
class EpisodesViewModel @Inject constructor(
    private val appReadDao: AppReadDao,
    private val watchStateWriter: EpisodeWatchStateWriter
) : ViewModel() {
    private var showId: Int? = null
    private var seasonNumber: Int? = null
    
    private val _episodes = MutableStateFlow<List<Episode>>(emptyList())
    val episodes: StateFlow<List<Episode>> = _episodes.asStateFlow()
    
    fun initialize(showId: Int, seasonNumber: Int) {
        if (this.showId == showId && this.seasonNumber == seasonNumber) return
        this.showId = showId
        this.seasonNumber = seasonNumber
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
        val targetShowId = showId ?: return emptyList()
        val targetSeasonNumber = seasonNumber ?: return emptyList()
        return appReadDao.getEpisodesForSeason(targetShowId, targetSeasonNumber).map { row ->
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
            val targetShowId = showId ?: return@launch
            val targetSeasonNumber = seasonNumber ?: return@launch
            watchStateWriter.setSeasonWatched(targetShowId, targetSeasonNumber, watched)
            
            // Reload to reflect changes
            withContext(Dispatchers.Main) {
                loadEpisodes()
            }
        }
    }
}
