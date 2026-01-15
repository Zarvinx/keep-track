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
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
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
    private val contentResolver: ContentResolver = application.contentResolver
    
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
        val uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, showId.toString())
        val projection = arrayOf(
            ShowsTable.COLUMN_NAME,
            ShowsTable.COLUMN_STARRED,
            ShowsTable.COLUMN_ARCHIVED,
            ShowsTable.COLUMN_POSTER_PATH,
            ShowsTable.COLUMN_OVERVIEW,
            ShowsTable.COLUMN_FIRST_AIRED
        )
        
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_NAME)
                val starredIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_STARRED)
                val archivedIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_ARCHIVED)
                val posterIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_POSTER_PATH)
                val overviewIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_OVERVIEW)
                val firstAiredIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_FIRST_AIRED)
                
                return ShowDetails(
                    name = cursor.getString(nameIndex),
                    starred = cursor.getInt(starredIndex) > 0,
                    archived = cursor.getInt(archivedIndex) > 0,
                    posterPath = cursor.getString(posterIndex),
                    overview = if (cursor.isNull(overviewIndex)) null else cursor.getString(overviewIndex),
                    firstAired = if (cursor.isNull(firstAiredIndex)) null else cursor.getLong(firstAiredIndex)
                )
            }
        }
        
        return null
    }
    
    fun toggleStarred() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _showDetails.value ?: return@launch
            val values = ContentValues().apply {
                put(ShowsTable.COLUMN_STARRED, if (current.starred) 0 else 1)
            }
            contentResolver.update(
                ShowsProvider.CONTENT_URI_SHOWS,
                values,
                "${ShowsTable.COLUMN_ID}=?",
                arrayOf(showId.toString())
            )
            loadShowDetails()
        }
    }
    
    fun toggleArchived() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _showDetails.value ?: return@launch
            val values = ContentValues().apply {
                put(ShowsTable.COLUMN_ARCHIVED, if (current.archived) 0 else 1)
            }
            contentResolver.update(
                ShowsProvider.CONTENT_URI_SHOWS,
                values,
                "${ShowsTable.COLUMN_ID}=?",
                arrayOf(showId.toString())
            )
            loadShowDetails()
        }
    }
    
    fun refreshShow() {
        AsyncTask().executeAsync(RefreshShowTask(showId))
    }
    
    fun markShowWatched(watched: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(EpisodesTable.COLUMN_WATCHED, if (watched) 1 else 0)
            }
            contentResolver.update(
                ShowsProvider.CONTENT_URI_EPISODES,
                values,
                "${EpisodesTable.COLUMN_SHOW_ID}=? AND ${EpisodesTable.COLUMN_SEASON_NUMBER}!=?",
                arrayOf(showId.toString(), "0")
            )
        }
    }
    
    fun deleteShow() {
        AsyncTask().executeAsync(DeleteShowTask(showId))
    }
}
