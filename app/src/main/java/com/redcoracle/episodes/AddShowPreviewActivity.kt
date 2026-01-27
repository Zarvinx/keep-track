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

package com.redcoracle.episodes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redcoracle.episodes.services.AddShowTask
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.ui.AddShowPreviewScreen
import com.redcoracle.episodes.ui.AddShowSearchViewModel
import com.redcoracle.episodes.ui.AddShowSearchViewModelFactory
import com.redcoracle.episodes.ui.theme.EpisodesTheme

class AddShowPreviewActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val searchResultIndex = intent.getIntExtra("searchResultIndex", 0)
        
        setContent {
            EpisodesTheme {
                AddShowPreviewScaffold(
                    searchResultIndex = searchResultIndex,
                    onNavigateBack = { finish() },
                    onAddShow = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShowPreviewScaffold(
    searchResultIndex: Int,
    onNavigateBack: () -> Unit,
    onAddShow: () -> Unit
) {
    // We need to get the search results from the shared ViewModel
    // Since we came from AddShowSearchActivity, the results should be cached
    val context = LocalContext.current
    
    // Get the show from AddShowSearchResults singleton (legacy approach)
    // TODO: Replace with proper state management when fully migrated
    val show = remember {
        AddShowSearchResults.data?.getOrNull(searchResultIndex)
    }
    
    if (show == null) {
        // Data was destroyed, navigate back
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(show.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Add the show
                        AsyncTask().executeAsync(
                            AddShowTask(show.id, show.name, show.language)
                        )
                        onAddShow()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add show")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AddShowPreviewScreen(show = show)
        }
    }
}
