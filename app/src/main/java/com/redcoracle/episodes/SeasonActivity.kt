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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redcoracle.episodes.db.room.AppDatabase
import com.redcoracle.episodes.ui.EpisodesListScreen
import com.redcoracle.episodes.ui.EpisodesViewModel
import com.redcoracle.episodes.ui.EpisodesViewModelFactory
import com.redcoracle.episodes.ui.theme.EpisodesTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SeasonActivity : ComponentActivity() {
    
    private var showId: Int = -1
    private var seasonNumber: Int = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        showId = intent.getIntExtra("showId", -1)
        if (showId == -1) {
            throw IllegalArgumentException("must provide valid showId")
        }
        
        seasonNumber = intent.getIntExtra("seasonNumber", -1)
        if (seasonNumber == -1) {
            throw IllegalArgumentException("must provide valid seasonNumber")
        }
        
        setContent {
            EpisodesTheme {
                SeasonScreen(
                    showId = showId,
                    seasonNumber = seasonNumber,
                    onNavigateBack = { finish() },
                    onEpisodeSelected = { episodeId -> onEpisodeSelected(episodeId) }
                )
            }
        }
    }
    
    private fun onEpisodeSelected(episodeId: Int) {
        val intent = Intent(this, EpisodeActivity::class.java)
        intent.putExtra("showId", showId)
        intent.putExtra("seasonNumber", seasonNumber)
        intent.putExtra("initialEpisodeId", episodeId)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonScreen(
    showId: Int,
    seasonNumber: Int,
    onNavigateBack: () -> Unit,
    onEpisodeSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val viewModel: EpisodesViewModel = viewModel(
        factory = EpisodesViewModelFactory(
            application = context.applicationContext as android.app.Application,
            showId = showId,
            seasonNumber = seasonNumber
        ),
        key = "episodes_${showId}_$seasonNumber"
    )
    
    var showMenu by remember { mutableStateOf(false) }
    var showName by remember { mutableStateOf("") }
    
    // Load show name
    LaunchedEffect(showId) {
        showName = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context.applicationContext).showQueriesDao()
                .getShowNameById(showId)
                ?: ""
        }
    }
    
    val seasonName = if (seasonNumber == 0) {
        stringResource(R.string.season_name_specials)
    } else {
        stringResource(R.string.season_name, seasonNumber)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(showName)
                        Text(
                            text = seasonName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_mark_season_watched)) },
                            onClick = {
                                showMenu = false
                                viewModel.markAllWatched(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_mark_season_not_watched)) },
                            onClick = {
                                showMenu = false
                                viewModel.markAllWatched(false)
                            }
                        )
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
            EpisodesListScreen(
                showId = showId,
                seasonNumber = seasonNumber,
                onEpisodeClick = onEpisodeSelected
            )
        }
    }
}
