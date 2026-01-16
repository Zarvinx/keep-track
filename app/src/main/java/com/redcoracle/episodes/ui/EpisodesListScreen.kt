/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redcoracle.episodes.R
import java.text.DateFormat
import java.util.Date

@Composable
fun EpisodesListScreen(
    showId: Int,
    seasonNumber: Int,
    onEpisodeClick: (Int) -> Unit
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
    
    val episodes by viewModel.episodes.collectAsState()
    
    if (episodes.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No episodes found",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = episodes,
                key = { it.id },
                contentType = { "episode_item" }
            ) { episode ->
                EpisodeListItem(
                    episode = episode,
                    seasonNumber = seasonNumber,
                    onClick = { onEpisodeClick(episode.id) },
                    onWatchedChange = { watched ->
                        viewModel.toggleEpisodeWatched(episode.id, watched)
                    }
                )
            }
        }
    }
}

@Composable
fun EpisodeListItem(
    episode: Episode,
    seasonNumber: Int,
    onClick: () -> Unit,
    onWatchedChange: (Boolean) -> Unit
) {
    val now = Date()
    val isUpcoming = episode.firstAired?.let { Date(it * 1000).after(now) } ?: false
    
    val textColor = if (isUpcoming) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Episode name
                Text(
                    text = if (seasonNumber == 0) {
                        episode.name
                    } else {
                        "${episode.episodeNumber} - ${episode.name}"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = textColor
                )
                
                // Air date
                episode.firstAired?.let { timestamp ->
                    val date = Date(timestamp * 1000)
                    val dateText = DateFormat.getDateInstance().format(date)
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Watched checkbox
            Checkbox(
                checked = episode.watched,
                onCheckedChange = onWatchedChange
            )
        }
    }
}
