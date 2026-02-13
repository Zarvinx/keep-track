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

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.room.EpisodeWatchStateWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NextEpisode(
    val id: Int,
    val name: String,
    val overview: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val firstAired: Long?,
    val watched: Boolean
)

class NextEpisodeViewModel(application: Application, private val showId: Int) : AndroidViewModel(application) {
    private val contentResolver: ContentResolver = application.contentResolver
    private val watchStateWriter = EpisodeWatchStateWriter(application.applicationContext)
    
    private val _nextEpisode = MutableStateFlow<NextEpisode?>(null)
    val nextEpisode: StateFlow<NextEpisode?> = _nextEpisode.asStateFlow()
    
    init {
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
        val projection = arrayOf(
            EpisodesTable.COLUMN_ID,
            EpisodesTable.COLUMN_NAME,
            EpisodesTable.COLUMN_OVERVIEW,
            EpisodesTable.COLUMN_SEASON_NUMBER,
            EpisodesTable.COLUMN_EPISODE_NUMBER,
            EpisodesTable.COLUMN_FIRST_AIRED,
            EpisodesTable.COLUMN_WATCHED
        )
        
        // Query for the first unwatched episode (excluding season 0)
        val selection = "${EpisodesTable.COLUMN_SHOW_ID}=? AND ${EpisodesTable.COLUMN_SEASON_NUMBER}!=0 AND (${EpisodesTable.COLUMN_WATCHED}==0 OR ${EpisodesTable.COLUMN_WATCHED} IS NULL)"
        val selectionArgs = arrayOf(showId.toString())
        val sortOrder = "${EpisodesTable.COLUMN_SEASON_NUMBER} ASC, ${EpisodesTable.COLUMN_EPISODE_NUMBER} ASC LIMIT 1"
        
        contentResolver.query(
            ShowsProvider.CONTENT_URI_EPISODES,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_NAME)
                val overviewIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_OVERVIEW)
                val seasonIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER)
                val episodeIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER)
                val firstAiredIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_FIRST_AIRED)
                val watchedIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED)
                
                return NextEpisode(
                    id = cursor.getInt(idIndex),
                    name = cursor.getString(nameIndex),
                    overview = if (cursor.isNull(overviewIndex)) null else cursor.getString(overviewIndex),
                    seasonNumber = cursor.getInt(seasonIndex),
                    episodeNumber = cursor.getInt(episodeIndex),
                    firstAired = if (cursor.isNull(firstAiredIndex)) null else cursor.getLong(firstAiredIndex),
                    watched = cursor.getInt(watchedIndex) > 0
                )
            }
        }
        
        return null
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
