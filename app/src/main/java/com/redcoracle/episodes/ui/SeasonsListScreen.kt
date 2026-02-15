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
 
package com.redcoracle.episodes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.redcoracle.episodes.R

@Composable
fun SeasonsListScreen(
    showId: Int,
    onSeasonClick: (Int) -> Unit
) {
    val viewModel: SeasonsViewModel = hiltViewModel()
    LaunchedEffect(showId) {
        viewModel.initialize(showId)
    }
    
    val seasons by viewModel.seasons.collectAsState()

    if (seasons.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No seasons found",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(seasons, key = { it.seasonNumber }) { season ->
                SeasonListItem(
                    season = season,
                    onClick = { onSeasonClick(season.seasonNumber) }
                )
            }
        }
    }
}

@Composable
fun SeasonListItem(
    season: Season,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Season name
            Text(
                text = season.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            // Progress bar
            if (season.airedCount > 0) {
                val progress = season.watchedCount.toFloat() / season.airedCount.toFloat()
                val countTextColor = if (season.watchedCount == 0) {
                    Color(0xFF1A1A1A)
                } else if (MaterialTheme.colorScheme.primary.luminance() < 0.5f) {
                    Color(0xFFF2F2F2)
                } else {
                    Color(0xFF1A1A1A)
                }
                val countTextShadowColor = if (countTextColor.luminance() > 0.5f) {
                    Color.Black.copy(alpha = 0.75f)
                } else {
                    Color.White.copy(alpha = 0.45f)
                }
                
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    
                    // Watched count text overlay
                    val countText = if (season.upcomingCount > 0) {
                        stringResource(R.string.watched_count, season.watchedCount, season.airedCount) +
                                " " + stringResource(R.string.upcoming_count, season.upcomingCount)
                    } else {
                        stringResource(R.string.watched_count, season.watchedCount, season.airedCount)
                    }
                    
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            shadow = Shadow(
                                color = countTextShadowColor,
                                offset = Offset(1f, 1f),
                                blurRadius = 3f
                            )
                        ),
                        color = countTextColor,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                    )
                }
            } else if (season.upcomingCount > 0) {
                // Show upcoming count for seasons with no aired episodes yet
                Text(
                    text = stringResource(R.string.upcoming_count, season.upcomingCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
