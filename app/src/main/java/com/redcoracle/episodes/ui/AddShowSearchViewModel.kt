/*
 * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
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
import com.redcoracle.episodes.Preferences
import com.redcoracle.episodes.tvdb.Client
import com.redcoracle.episodes.tvdb.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val results: List<Show>) : SearchState()
    data class Error(val message: String) : SearchState()
}

@HiltViewModel
class AddShowSearchViewModel @Inject constructor() : ViewModel() {
    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    
    private var initialized = false

    fun initialize(initialQuery: String) {
        if (initialized) return
        initialized = true
        _query.value = initialQuery
        searchShows()
    }
    
    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }
    
    fun searchShows() {
        val currentQuery = _query.value
        if (currentQuery.isBlank()) {
            _searchState.value = SearchState.Success(emptyList())
            return
        }
        
        viewModelScope.launch {
            _searchState.value = SearchState.Loading
            
            try {
                val results = withContext(Dispatchers.IO) {
                    val tmdbClient = Client()
                    val preferences = Preferences.getSharedPreferences()
                    val language = preferences?.getString("pref_language", "en") ?: "en"
                    
                    var searchResults = tmdbClient.searchShows(currentQuery, language)
                    
                    // If no results, try substituting " and " with " & "
                    if (searchResults.isEmpty() && currentQuery.contains(" and ")) {
                        searchResults = tmdbClient.searchShows(currentQuery.replace(" and ", " & "), null)
                    }
                    
                    // If still no results, search all languages
                    if (searchResults.isEmpty()) {
                        searchResults = tmdbClient.searchShows(currentQuery, null)
                    }
                    
                    searchResults
                }
                
                _searchState.value = SearchState.Success(results)
            } catch (e: Exception) {
                _searchState.value = SearchState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
}
