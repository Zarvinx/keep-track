/*
 * Copyright (C) 2013 Jamie Nicol <jamie@thenicols.net>
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

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.redcoracle.episodes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    showId: Int,
    onNavigateBack: () -> Unit,
    onSeasonSelected: (Int) -> Unit
) {
    val viewModel: ShowViewModel = hiltViewModel()
    LaunchedEffect(showId) {
        viewModel.initialize(showId)
    }
    
    val showDetails by viewModel.showDetails.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { 
                    MarqueeText(
                        text = showDetails?.name ?: "",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Star toggle
                    IconButton(onClick = { viewModel.toggleStarred() }) {
                        Icon(
                            imageVector = if (showDetails?.starred == true) {
                                Icons.Filled.Star
                            } else {
                                Icons.Default.StarBorder
                            },
                            contentDescription = if (showDetails?.starred == true) "Unstar" else "Star",
                            tint = if (showDetails?.starred == true) Color(0xFFFFD700) else LocalContentColor.current
                        )
                    }
                    
                    // Archive toggle
                    IconButton(onClick = { viewModel.toggleArchived() }) {
                        Icon(
                            painter = if (showDetails?.archived == true) {
                                painterResource(R.drawable.ic_show_archived)
                            } else {
                                painterResource(R.drawable.ic_show_unarchived)
                            },
                            contentDescription = if (showDetails?.archived == true) "Unarchive" else "Archive"
                        )
                    }
                    
                    // More menu
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_refresh_show)) },
                            onClick = {
                                showMenu = false
                                viewModel.refreshShow()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_mark_show_watched)) },
                            onClick = {
                                showMenu = false
                                viewModel.markShowWatched(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_mark_show_not_watched)) },
                            onClick = {
                                showMenu = false
                                viewModel.markShowWatched(false)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_delete_show)) },
                            onClick = {
                                showMenu = false
                                viewModel.deleteShow()
                                onNavigateBack()
                            }
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header image
            showDetails?.posterPath?.let { posterPath ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w500/$posterPath",
                        contentDescription = showDetails?.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        placeholder = painterResource(R.drawable.blank_show_banner),
                        error = painterResource(R.drawable.blank_show_banner)
                    )
                }
            }
            
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.show_tab_overview)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.show_tab_episodes)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(stringResource(R.string.show_tab_next)) }
                )
            }
            
            // Tab content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> ShowOverviewTab(showId)
                    1 -> SeasonsListTab(showId, onSeasonSelected)
                    2 -> NextEpisodeTab(showId)
                }
            }
        }
    }
}

@Composable
fun ShowOverviewTab(showId: Int) {
    ShowOverviewScreen(showId = showId)
}

@Composable
fun SeasonsListTab(showId: Int, onSeasonSelected: (Int) -> Unit) {
    SeasonsListScreen(
        showId = showId,
        onSeasonClick = onSeasonSelected
    )
}

@Composable
fun NextEpisodeTab(showId: Int) {
    NextEpisodeScreen(
        showId = showId
    )
}
