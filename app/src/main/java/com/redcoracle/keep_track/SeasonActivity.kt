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

package com.redcoracle.keep_track

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.redcoracle.keep_track.db.room.AppDatabase
import com.redcoracle.keep_track.ui.EpisodesListScreen
import com.redcoracle.keep_track.ui.EpisodesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonScreen(
    showId: Int,
    seasonNumber: Int,
    onNavigateBack: () -> Unit,
    onEpisodeSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val viewModel: EpisodesViewModel = hiltViewModel()
    LaunchedEffect(showId, seasonNumber) {
        viewModel.initialize(showId, seasonNumber)
    }

    var showMenu by remember { mutableStateOf(false) }
    var showName by remember { mutableStateOf("") }

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
