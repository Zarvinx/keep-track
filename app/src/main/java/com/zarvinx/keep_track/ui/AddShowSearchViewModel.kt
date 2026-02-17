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

package com.zarvinx.keep_track.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zarvinx.keep_track.Preferences
import com.zarvinx.keep_track.R
import com.zarvinx.keep_track.tvdb.Client
import com.zarvinx.keep_track.tvdb.Show
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Success(val results: List<Show>) : SearchState()
    data class Error(val messageResId: Int) : SearchState()
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
                _searchState.value = SearchState.Error(mapSearchErrorMessage(e))
            }
        }
    }

    private fun mapSearchErrorMessage(error: Throwable): Int {
        val message = buildString {
            append(error.message.orEmpty())
            var cause = error.cause
            while (cause != null) {
                append(" ")
                append(cause.message.orEmpty())
                cause = cause.cause
            }
        }.lowercase()

        return when {
            error is UnknownHostException || error is SocketTimeoutException ->
                R.string.search_error_network
            error is IOException && (message.contains("timeout") || message.contains("unable to resolve host")) ->
                R.string.search_error_network
            message.contains("429") || message.contains("rate limit") || message.contains("too many requests") ->
                R.string.search_error_rate_limited
            message.contains("500") || message.contains("502") || message.contains("503") || message.contains("504") ->
                R.string.search_error_server
            else -> R.string.search_error_generic
        }
    }
}
