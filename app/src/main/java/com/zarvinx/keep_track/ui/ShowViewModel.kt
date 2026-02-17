/*
 * Copyright (C) 2013 Jamie Nicol <jamie@thenicols.net>
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

package com.zarvinx.keep_track.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zarvinx.keep_track.R
import com.zarvinx.keep_track.db.room.ShowQueriesDao
import com.zarvinx.keep_track.db.room.EpisodeWatchStateWriter
import com.zarvinx.keep_track.db.room.ShowMutationsWriter
import com.zarvinx.keep_track.services.AsyncTask
import com.zarvinx.keep_track.services.DeleteShowTask
import com.zarvinx.keep_track.services.RefreshShowTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ShowDetails(
    val name: String,
    val starred: Boolean,
    val archived: Boolean,
    val posterPath: String?,
    val overview: String?,
    val firstAired: Long?
)

sealed class ShowUiEvent {
    data class Error(val messageResId: Int) : ShowUiEvent()
}

@HiltViewModel
class ShowViewModel @Inject constructor(
    private val showQueriesDao: ShowQueriesDao,
    private val watchStateWriter: EpisodeWatchStateWriter,
    private val showMutationsWriter: ShowMutationsWriter
) : ViewModel() {
    private var showId: Int? = null
    
    private val _showDetails = MutableStateFlow<ShowDetails?>(null)
    val showDetails: StateFlow<ShowDetails?> = _showDetails.asStateFlow()
    private val _uiEvents = MutableSharedFlow<ShowUiEvent>(extraBufferCapacity = 4)
    val uiEvents: SharedFlow<ShowUiEvent> = _uiEvents.asSharedFlow()
    
    fun initialize(showId: Int) {
        if (this.showId == showId) return
        this.showId = showId
        loadShowDetails()
    }
    
    fun loadShowDetails() {
        viewModelScope.launch {
            val details = withContext(Dispatchers.IO) {
                loadShowFromDatabase()
            }
            _showDetails.value = details
        }
    }
    
    private fun loadShowFromDatabase(): ShowDetails? {
        val targetShowId = showId ?: return null
        val row = showQueriesDao.getShowDetailsById(targetShowId) ?: return null
        return ShowDetails(
            name = row.name,
            starred = (row.starred ?: 0) > 0,
            archived = (row.archived ?: 0) > 0,
            posterPath = row.posterPath,
            overview = row.overview,
            firstAired = row.firstAired
        )
    }
    
    fun toggleStarred() {
        viewModelScope.launch(Dispatchers.IO) {
            val targetShowId = showId ?: return@launch
            val current = _showDetails.value ?: return@launch
            showMutationsWriter.setStarred(targetShowId, !current.starred)
            loadShowDetails()
        }
    }
    
    fun toggleArchived() {
        viewModelScope.launch(Dispatchers.IO) {
            val targetShowId = showId ?: return@launch
            val current = _showDetails.value ?: return@launch
            showMutationsWriter.setArchived(targetShowId, !current.archived)
            loadShowDetails()
        }
    }
    
    fun refreshShow() {
        val targetShowId = showId ?: return
        AsyncTask().executeAsync(
            RefreshShowTask(targetShowId),
            onError = {
                _uiEvents.tryEmit(ShowUiEvent.Error(R.string.refresh_show_error_message))
            }
        )
    }
    
    fun markShowWatched(watched: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val targetShowId = showId ?: return@launch
            watchStateWriter.setShowWatched(targetShowId, watched)
        }
    }
    
    fun deleteShow() {
        val targetShowId = showId ?: return
        AsyncTask().executeAsync(
            DeleteShowTask(targetShowId),
            onError = {
                _uiEvents.tryEmit(ShowUiEvent.Error(R.string.delete_show_error_message))
            }
        )
    }
}
