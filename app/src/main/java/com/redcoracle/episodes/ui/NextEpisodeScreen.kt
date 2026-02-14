/*
 * Copyright (C) 2015 Jamie Nicol <jamie@thenicols.net>
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redcoracle.episodes.R
import java.text.DateFormat
import java.util.*

@Composable
fun NextEpisodeScreen(
    showId: Int
) {
    val viewModel: NextEpisodeViewModel = hiltViewModel()
    LaunchedEffect(showId) {
        viewModel.initialize(showId)
    }
    
    val episode by viewModel.nextEpisode.collectAsState()
    val scrollState = rememberScrollState()
    
    if (episode == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No unwatched episodes",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        episode?.let { ep ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Title with season/episode prefix
                Text(
                    text = buildString {
                        if (ep.seasonNumber != 0) {
                            append(stringResource(R.string.season_episode_prefix, ep.seasonNumber, ep.episodeNumber))
                        }
                        append(ep.name)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Date and watched checkbox row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Air date
                    ep.firstAired?.let { timestamp ->
                        val date = Date(timestamp * 1000)
                        val dateText = DateFormat.getDateInstance(DateFormat.LONG).format(date)
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Watched checkbox
                    val now = System.currentTimeMillis()
                    val isWatchable = ep.firstAired?.let { it * 1000 <= now } ?: false
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.watched_check_box),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isWatchable) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            }
                        )
                        if (!isWatchable) {
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Checkbox(
                            checked = ep.watched,
                            onCheckedChange = if (isWatchable) { isChecked ->
                                viewModel.setWatched(isChecked)
                            } else null,
                            enabled = isWatchable
                        )
                    }
                }
                
                // Overview
                ep.overview?.let { overview ->
                    if (overview.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
