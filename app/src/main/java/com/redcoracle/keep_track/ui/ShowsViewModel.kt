package com.redcoracle.keep_track.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcoracle.keep_track.db.room.EpisodeCountRow
import com.redcoracle.keep_track.db.room.ShowListRow
import com.redcoracle.keep_track.db.room.AppReadDao
import com.redcoracle.keep_track.db.room.EpisodeWatchStateWriter
import com.redcoracle.keep_track.db.room.ShowMutationsWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Stable
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
    val nextEpisodeNumber: Int?,
    val nextEpisodeAirDate: Long?,
    val status: String?
)

@HiltViewModel
class ShowsViewModel @Inject constructor(
    private val appReadDao: AppReadDao,
    private val watchStateWriter: EpisodeWatchStateWriter,
    private val showMutationsWriter: ShowMutationsWriter,
    private val prefs: SharedPreferences
) : ViewModel() {
    
    private val _shows = MutableStateFlow<List<Show>>(emptyList())
    val shows: StateFlow<List<Show>> = _shows.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(0)
    val currentFilter: StateFlow<Int> = _currentFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    companion object {
        const val SHOWS_FILTER_WATCHING = 0
        const val SHOWS_FILTER_STARRED = 1
        const val SHOWS_FILTER_ARCHIVED = 2
        const val SHOWS_FILTER_UPCOMING = 3
        const val SHOWS_FILTER_ALL = 4
        const val KEY_PREF_SHOWS_FILTER = "pref_shows_filter"
    }
    
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KEY_PREF_SHOWS_FILTER) {
            _currentFilter.value = prefs.getInt(KEY_PREF_SHOWS_FILTER, SHOWS_FILTER_WATCHING)
        }
    }
    
    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        _currentFilter.value = prefs.getInt(KEY_PREF_SHOWS_FILTER, SHOWS_FILTER_WATCHING)
        
        viewModelScope.launch {
            combine(
                appReadDao.observeShowsForList(),
                appReadDao.observeEpisodeCounts(),
                _currentFilter,
                _searchQuery
            ) { showRows, episodeRows, filter, searchQuery ->
                withContext(Dispatchers.IO) {
                    loadShows(showRows, episodeRows, filter, searchQuery)
                }
            }.collect { showsList ->
                _shows.value = showsList
            }
        }
    }
    
    private fun loadShows(
        showRows: List<ShowListRow>,
        episodeRows: List<EpisodeCountRow>,
        filter: Int,
        searchQuery: String
    ): List<Show> {
        val showsList = mutableListOf<Show>()
        val episodeCounts = calculateEpisodeCounts(episodeRows)

        for (row in showRows) {
            val id = row.id
            val name = row.name.orEmpty()
            val starred = (row.starred ?: 0) > 0
            val archived = (row.archived ?: 0) > 0
            val bannerPath = row.bannerPath
            val status = row.status

            val counts = episodeCounts[id] ?: EpisodeCounts(0, 0, 0)

            val shouldInclude = when (filter) {
                SHOWS_FILTER_WATCHING -> !archived
                SHOWS_FILTER_STARRED -> starred
                SHOWS_FILTER_ARCHIVED -> archived
                SHOWS_FILTER_UPCOMING -> counts.upcoming > 0 && counts.watched == counts.aired && !archived
                else -> true
            }

            val matchesSearch = searchQuery.isBlank() || name.contains(searchQuery, ignoreCase = true)

            if (shouldInclude && matchesSearch) {
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
                        nextEpisodeNumber = nextEpisode?.episodeNumber,
                        nextEpisodeAirDate = nextEpisode?.airDate,
                        status = status
                    )
                )
            }
        }
        
        // Sort shows:
        // 1. Starred shows at the top, sorted by name
        // 2. Unfinished shows (watching), sorted by name
        // 3. Finished shows (100% complete), sorted by name
        return showsList.sortedWith(
            compareBy<Show> { show ->
                // First: starred = 0, not starred = 1 (starred shows first)
                if (show.starred) 0 else 1
            }.thenBy { show ->
                // Second: finished = 1, not finished = 0 (unfinished shows before finished)
                val isFinished = show.totalCount > 0 && show.watchedCount == show.totalCount
                if (isFinished) 1 else 0
            }.thenBy { show ->
                // Third: sort alphabetically by name (case-insensitive)
                show.name.lowercase()
            }
        )
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
        val episodeNumber: Int,
        val airDate: Long?
    )
    
    private fun getNextEpisode(showId: Int): NextEpisodeInfo? {
        val row = appReadDao.getNextEpisodeInfo(showId) ?: return null
        val airDateSeconds = row.firstAired ?: 0L
        return NextEpisodeInfo(
            id = row.id,
            name = row.name.orEmpty(),
            seasonNumber = row.seasonNumber ?: 0,
            episodeNumber = row.episodeNumber ?: 0,
            airDate = if (airDateSeconds > 0) airDateSeconds * 1000 else null
        )
    }
    
    private fun calculateEpisodeCounts(rows: List<EpisodeCountRow>): Map<Int, EpisodeCounts> {
        val counts = mutableMapOf<Int, EpisodeCounts>()
        val now = System.currentTimeMillis() / 1000

        for (row in rows) {
            val showId = row.showId ?: continue
            val watched = (row.watched ?: 0) > 0
            val firstAired = row.firstAired ?: 0L

            val current = counts.getOrDefault(showId, EpisodeCounts(0, 0, 0))

            val isAired = firstAired > 0 && firstAired <= now
            val isUpcoming = firstAired > now

            counts[showId] = EpisodeCounts(
                watched = current.watched + if (watched) 1 else 0,
                aired = current.aired + if (isAired) 1 else 0,
                upcoming = current.upcoming + if (isUpcoming) 1 else 0
            )
        }

        return counts
    }
    
    fun toggleStarred(showId: Int, starred: Boolean) {
        viewModelScope.launch {
            // Optimistically update UI immediately
            _shows.value = _shows.value.map { show ->
                if (show.id == showId) show.copy(starred = starred) else show
            }
            
            // Then update database in background
            withContext(Dispatchers.IO) {
                showMutationsWriter.setStarred(showId, starred)
            }
        }
    }
    
    fun toggleArchived(showId: Int, archived: Boolean) {
        viewModelScope.launch {
            // Optimistically update UI immediately
            _shows.value = _shows.value.map { show ->
                if (show.id == showId) show.copy(archived = archived) else show
            }
            
            // Then update database in background
            withContext(Dispatchers.IO) {
                showMutationsWriter.setArchived(showId, archived)
            }
        }
    }
    
    fun markEpisodeWatched(episodeId: Int, watched: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            watchStateWriter.setEpisodeWatched(episodeId, watched)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
}
