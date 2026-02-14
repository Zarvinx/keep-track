/*
 * Copyright (C) 2012-2015 Jamie Nicol <jamie@thenicols.net>
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.redcoracle.episodes.ui.EpisodeDetailsScreen
import com.redcoracle.episodes.ui.EpisodesViewModel
import com.redcoracle.episodes.ui.theme.EpisodesTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EpisodeActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val showId = intent.getIntExtra("showId", -1)
        if (showId == -1) {
            throw IllegalArgumentException("must provide valid showId")
        }
        
        val seasonNumber = intent.getIntExtra("seasonNumber", -1)
        if (seasonNumber == -1) {
            throw IllegalArgumentException("must provide valid seasonNumber")
        }
        
        val episodeId = intent.getIntExtra("initialEpisodeId", -1)
        if (episodeId == -1) {
            throw IllegalArgumentException("must provide valid initialEpisodeId")
        }
        
        setContent {
            EpisodesTheme {
                EpisodeScreen(
                    showId = showId,
                    seasonNumber = seasonNumber,
                    episodeId = episodeId,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeScreen(
    showId: Int,
    seasonNumber: Int,
    episodeId: Int,
    onNavigateBack: () -> Unit
) {
    val viewModel: EpisodesViewModel = hiltViewModel()
    LaunchedEffect(showId, seasonNumber) {
        viewModel.initialize(showId, seasonNumber)
    }
    
    val episodes by viewModel.episodes.collectAsState()
    val episode = episodes.find { it.id == episodeId }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Episode Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            if (episode != null) {
                EpisodeDetailsScreen(
                    episode = episode,
                    onWatchedChange = { watched ->
                        viewModel.toggleEpisodeWatched(episode.id, watched)
                    }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
