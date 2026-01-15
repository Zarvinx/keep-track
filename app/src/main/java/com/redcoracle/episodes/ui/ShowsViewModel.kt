package com.redcoracle.episodes.ui

import android.app.Application
import android.content.ContentResolver
import android.content.SharedPreferences
import android.database.Cursor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Show(
    val id: Int,
    val name: String,
    val bannerPath: String?,
    val starred: Boolean,
    val archived: Boolean,
    val watchedCount: Int,
    val totalCount: Int,
    val upcomingCount: Int,
    val nextEpisodeId: Int?,
    val nextEpisodeName: String?,
    val nextEpisodeSeasonNumber: Int?,
    val nextEpisodeNumber: Int?
)

class ShowsViewModel(application: Application) : AndroidViewModel(application) {
    private val contentResolver: ContentResolver = application.contentResolver
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    
    private val _shows = MutableStateFlow<List<Show>>(emptyList())
    val shows: StateFlow<List<Show>> = _shows.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(0)
    val currentFilter: StateFlow<Int> = _currentFilter.asStateFlow()
    
    companion object {
        const val SHOWS_FILTER_ALL = 0
        const val SHOWS_FILTER_STARRED = 1
        const val SHOWS_FILTER_UNCOMPLETED = 2
        const val SHOWS_FILTER_ARCHIVED = 3
        const val SHOWS_FILTER_UPCOMING = 4
        const val KEY_PREF_SHOWS_FILTER = "pref_shows_filter"
    }
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_PREF_SHOWS_FILTER) {
            _currentFilter.value = prefs.getInt(KEY_PREF_SHOWS_FILTER, SHOWS_FILTER_ALL)
            loadShows()
        }
    }
    
    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        _currentFilter.value = prefs.getInt(KEY_PREF_SHOWS_FILTER, SHOWS_FILTER_ALL)
        loadShows()
    }
    
    fun loadShows() {
        viewModelScope.launch {
            val showsList = withContext(Dispatchers.IO) {
                loadShowsFromDatabase()
            }
            _shows.value = showsList
        }
    }
    
    private fun loadShowsFromDatabase(): List<Show> {
        val showsList = mutableListOf<Show>()
        
        // Load shows
        val showsProjection = arrayOf(
            ShowsTable.COLUMN_ID,
            ShowsTable.COLUMN_NAME,
            ShowsTable.COLUMN_STARRED,
            ShowsTable.COLUMN_ARCHIVED,
            ShowsTable.COLUMN_BANNER_PATH
        )
        
        val showsCursor = contentResolver.query(
            ShowsProvider.CONTENT_URI_SHOWS,
            showsProjection,
            null,
            null,
            "${ShowsTable.COLUMN_STARRED} DESC, ${ShowsTable.COLUMN_NAME} COLLATE LOCALIZED ASC"
        )
        
        // Load episodes for counting
        val episodesProjection = arrayOf(
            EpisodesTable.COLUMN_SHOW_ID,
            EpisodesTable.COLUMN_SEASON_NUMBER,
            EpisodesTable.COLUMN_FIRST_AIRED,
            EpisodesTable.COLUMN_WATCHED
        )
        
        val episodesCursor = contentResolver.query(
            ShowsProvider.CONTENT_URI_EPISODES,
            episodesProjection,
            "${EpisodesTable.COLUMN_SEASON_NUMBER}!=?",
            arrayOf("0"),
            null
        )
        
        val episodeCounts = calculateEpisodeCounts(episodesCursor)
        episodesCursor?.close()
        
        showsCursor?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_NAME)
            val starredIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_STARRED)
            val archivedIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_ARCHIVED)
            val bannerIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_BANNER_PATH)
            
            while (cursor.moveToNext()) {
                val id = cursor.getInt(idIndex)
                val name = cursor.getString(nameIndex)
                val starred = cursor.getInt(starredIndex) > 0
                val archived = cursor.getInt(archivedIndex) > 0
                val bannerPath = cursor.getString(bannerIndex)
                
                val counts = episodeCounts[id] ?: EpisodeCounts(0, 0, 0)
                
                // Apply filter
                val filter = _currentFilter.value
                val shouldInclude = when (filter) {
                    SHOWS_FILTER_STARRED -> starred
                    SHOWS_FILTER_ARCHIVED -> archived
                    SHOWS_FILTER_UNCOMPLETED -> counts.watched < counts.aired && !archived
                    SHOWS_FILTER_UPCOMING -> counts.upcoming > 0 && counts.watched == counts.aired && !archived
                    else -> true // SHOWS_FILTER_ALL
                }
                
                if (shouldInclude) {
                    // Get next unwatched episode
                    val nextEpisode = getNextEpisode(id)
                    
                    showsList.add(
                        Show(
                            id = id,
                            name = name,
                            bannerPath = bannerPath,
                            starred = starred,
                            archived = archived,
                            watchedCount = counts.watched,
                            totalCount = counts.aired,
                            upcomingCount = counts.upcoming,
                            nextEpisodeId = nextEpisode?.id,
                            nextEpisodeName = nextEpisode?.name,
                            nextEpisodeSeasonNumber = nextEpisode?.seasonNumber,
                            nextEpisodeNumber = nextEpisode?.episodeNumber
                        )
                    )
                }
            }
        }
        
        return showsList
    }
    
    private data class EpisodeCounts(
        val watched: Int,
        val aired: Int,
        val upcoming: Int
    )
    
    private data class NextEpisodeInfo(
        val id: Int,
        val name: String,
        val seasonNumber: Int,
        val episodeNumber: Int
    )
    
    private fun getNextEpisode(showId: Int): NextEpisodeInfo? {
        val projection = arrayOf(
            EpisodesTable.COLUMN_ID,
            EpisodesTable.COLUMN_NAME,
            EpisodesTable.COLUMN_SEASON_NUMBER,
            EpisodesTable.COLUMN_EPISODE_NUMBER
        )
        
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
                val seasonIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER)
                val episodeIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER)
                
                return NextEpisodeInfo(
                    id = cursor.getInt(idIndex),
                    name = cursor.getString(nameIndex),
                    seasonNumber = cursor.getInt(seasonIndex),
                    episodeNumber = cursor.getInt(episodeIndex)
                )
            }
        }
        
        return null
    }
    
    private fun calculateEpisodeCounts(cursor: Cursor?): Map<Int, EpisodeCounts> {
        val counts = mutableMapOf<Int, EpisodeCounts>()
        
        cursor?.use {
            val showIdIndex = it.getColumnIndexOrThrow(EpisodesTable.COLUMN_SHOW_ID)
            val watchedIndex = it.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED)
            val firstAiredIndex = it.getColumnIndexOrThrow(EpisodesTable.COLUMN_FIRST_AIRED)
            
            val now = System.currentTimeMillis()
            
            while (it.moveToNext()) {
                val showId = it.getInt(showIdIndex)
                val watched = it.getInt(watchedIndex) > 0
                val firstAired = it.getLong(firstAiredIndex)
                
                val current = counts.getOrDefault(showId, EpisodeCounts(0, 0, 0))
                
                val isAired = firstAired > 0 && firstAired <= now
                val isUpcoming = firstAired > now
                
                counts[showId] = EpisodeCounts(
                    watched = current.watched + if (watched) 1 else 0,
                    aired = current.aired + if (isAired) 1 else 0,
                    upcoming = current.upcoming + if (isUpcoming) 1 else 0
                )
            }
        }
        
        return counts
    }
    
    fun toggleStarred(showId: Int, starred: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val values = android.content.ContentValues().apply {
                put(ShowsTable.COLUMN_STARRED, if (starred) 1 else 0)
            }
            contentResolver.update(
                ShowsProvider.CONTENT_URI_SHOWS,
                values,
                "${ShowsTable.COLUMN_ID}=?",
                arrayOf(showId.toString())
            )
            loadShows()
        }
    }
    
    fun toggleArchived(showId: Int, archived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val values = android.content.ContentValues().apply {
                put(ShowsTable.COLUMN_ARCHIVED, if (archived) 1 else 0)
            }
            contentResolver.update(
                ShowsProvider.CONTENT_URI_SHOWS,
                values,
                "${ShowsTable.COLUMN_ID}=?",
                arrayOf(showId.toString())
            )
            loadShows()
        }
    }
    
    fun markEpisodeWatched(episodeId: Int, watched: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = android.net.Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES, episodeId.toString())
            val values = android.content.ContentValues().apply {
                put(EpisodesTable.COLUMN_WATCHED, if (watched) 1 else 0)
            }
            
            contentResolver.update(uri, values, null, null)
            
            // Reload shows to update counts and next episode
            loadShows()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
}
