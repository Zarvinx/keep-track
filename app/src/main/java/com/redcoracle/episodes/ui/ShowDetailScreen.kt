package com.redcoracle.episodes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.redcoracle.episodes.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDetailScreen(
    showId: Int,
    onNavigateBack: () -> Unit,
    onSeasonSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ShowViewModel = viewModel(
        factory = ShowViewModelFactory(
            application = context.applicationContext as android.app.Application,
            showId = showId
        ),
        key = "show_$showId"
    )
    
    val showDetails by viewModel.showDetails.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { 
                    Text(
                        text = showDetails?.name ?: "",
                        maxLines = 2
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
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w1280/$posterPath",
                    contentDescription = showDetails?.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
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
                    1 -> SeasonsListTab(showId, onSeasonSelected, refreshKey)
                    2 -> NextEpisodeTab(showId, onEpisodeWatchedChanged = { refreshKey++ })
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
fun SeasonsListTab(showId: Int, onSeasonSelected: (Int) -> Unit, refreshKey: Int) {
    SeasonsListScreen(
        showId = showId,
        onSeasonClick = onSeasonSelected,
        refreshKey = refreshKey
    )
}

@Composable
fun NextEpisodeTab(showId: Int, onEpisodeWatchedChanged: () -> Unit) {
    NextEpisodeScreen(
        showId = showId,
        onEpisodeWatchedChanged = onEpisodeWatchedChanged
    )
}
