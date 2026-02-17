package com.zarvinx.keep_track

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.zarvinx.keep_track.ui.ShowsViewModel

@Stable
class MainScreenState(
    private val onSearchQueryChanged: (String) -> Unit
) {
    var isSearching by mutableStateOf(false)
    var showActionOverlay by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    fun updateSearch(query: String) {
        searchQuery = query
        onSearchQueryChanged(query)
    }

    fun startSearch() {
        showActionOverlay = true
        isSearching = true
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun closeSearch(keyboardController: SoftwareKeyboardController?) {
        isSearching = false
        updateSearch("")
        keyboardController?.hide()
    }
}

@Composable
fun rememberMainScreenState(viewModel: ShowsViewModel): MainScreenState {
    return remember(viewModel) {
        MainScreenState(onSearchQueryChanged = viewModel::setSearchQuery)
    }
}
