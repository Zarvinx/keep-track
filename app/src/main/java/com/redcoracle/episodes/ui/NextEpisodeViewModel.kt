/*
 * Copyright (C) 2015 Jamie Nicol <jamie@thenicols.net>
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

data class NextEpisode(
    val id: Int,
    val name: String,
    val overview: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val firstAired: Long?,
    val watched: Boolean
)

@HiltViewModel
class NextEpisodeViewModel @Inject constructor(
    private val appReadDao: AppReadDao,
    private val watchStateWriter: EpisodeWatchStateWriter
) : ViewModel() {
    private var showId: Int? = null
    
    private val _nextEpisode = MutableStateFlow<NextEpisode?>(null)
    val nextEpisode: StateFlow<NextEpisode?> = _nextEpisode.asStateFlow()
    
    fun initialize(showId: Int) {
        if (this.showId == showId) return
        this.showId = showId
        loadNextEpisode()
    }
    
    fun loadNextEpisode() {
        viewModelScope.launch {
            val episode = withContext(Dispatchers.IO) {
                loadNextEpisodeFromDatabase()
            }
            _nextEpisode.value = episode
        }
    }
    
    private fun loadNextEpisodeFromDatabase(): NextEpisode? {
        val targetShowId = showId ?: return null
        val row = appReadDao.getNextUnwatchedEpisode(targetShowId) ?: return null
        return NextEpisode(
            id = row.id,
            name = row.name.orEmpty(),
            overview = row.overview,
            seasonNumber = row.seasonNumber ?: 0,
            episodeNumber = row.episodeNumber ?: 0,
            firstAired = row.firstAired,
            watched = (row.watched ?: 0) > 0
        )
    }
    
    fun setWatched(watched: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val episode = _nextEpisode.value ?: return@launch
            watchStateWriter.setEpisodeWatched(episode.id, watched)
            
            // Reload to get the next unwatched episode
            loadNextEpisode()
        }
    }
}
