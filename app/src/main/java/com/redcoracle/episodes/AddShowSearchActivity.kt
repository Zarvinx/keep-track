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

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import com.redcoracle.episodes.ui.AddShowPreviewArgs
import com.redcoracle.episodes.ui.AddShowSearchScreen
import com.redcoracle.episodes.ui.AddShowSearchViewModel
import com.redcoracle.episodes.ui.theme.EpisodesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddShowSearchActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val query = intent.getStringExtra("query") ?: ""
        
        setContent {
            EpisodesTheme {
                AddShowSearchScaffold(
                    query = query,
                    onNavigateBack = { navigateToMain() },
                    onShowClick = { args -> navigateToPreview(args) }
                )
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToPreview(args: AddShowPreviewArgs) {
        startActivity(AddShowPreviewActivity.createIntent(this, args))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddShowSearchScaffold(
    query: String,
    onNavigateBack: () -> Unit,
    onShowClick: (AddShowPreviewArgs) -> Unit
) {
    val viewModel: AddShowSearchViewModel = hiltViewModel()
    
    val currentQuery by viewModel.query.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(query) {
        viewModel.initialize(query)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = currentQuery,
                        onValueChange = { viewModel.updateQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text("Search for shows...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.searchShows()
                                keyboardController?.hide()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            focusedIndicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                            disabledIndicatorColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                        ),
                        textStyle = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.searchShows() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
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
            AddShowSearchScreen(
                onShowClick = onShowClick,
                viewModel = viewModel
            )
        }
    }
}
