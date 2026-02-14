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

package com.redcoracle.episodes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redcoracle.episodes.db.room.AppDatabase
import com.redcoracle.episodes.db.room.EpisodeWatchStateWriter
import com.redcoracle.episodes.db.room.ShowMutationsWriter
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.DeleteShowTask
import com.redcoracle.episodes.services.RefreshShowTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ShowDetails(
    val name: String,
    val starred: Boolean,
    val archived: Boolean,
    val posterPath: String?,
    val overview: String?,
    val firstAired: Long?
)

class ShowViewModel(application: Application, private val showId: Int) : AndroidViewModel(application) {
    private val showQueriesDao = AppDatabase.getInstance(application.applicationContext).showQueriesDao()
    private val watchStateWriter = EpisodeWatchStateWriter(application.applicationContext)
    private val showMutationsWriter = ShowMutationsWriter(application.applicationContext)
    
    private val _showDetails = MutableStateFlow<ShowDetails?>(null)
    val showDetails: StateFlow<ShowDetails?> = _showDetails.asStateFlow()
    
    init {
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
        val row = showQueriesDao.getShowDetailsById(showId) ?: return null
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
            val current = _showDetails.value ?: return@launch
            showMutationsWriter.setStarred(showId, !current.starred)
            loadShowDetails()
        }
    }
    
    fun toggleArchived() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _showDetails.value ?: return@launch
            showMutationsWriter.setArchived(showId, !current.archived)
            loadShowDetails()
        }
    }
    
    fun refreshShow() {
        AsyncTask().executeAsync(RefreshShowTask(showId))
    }
    
    fun markShowWatched(watched: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            watchStateWriter.setShowWatched(showId, watched)
        }
    }
    
    fun deleteShow() {
        AsyncTask().executeAsync(DeleteShowTask(showId))
    }
}
