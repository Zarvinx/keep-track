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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.redcoracle.episodes.R

@Composable
fun ShowsListScreen(
    viewModel: ShowsViewModel = viewModel(),
    onShowClick: (Int) -> Unit
) {
    val shows by viewModel.shows.collectAsState()
    
    // Reload shows when the screen is displayed
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.loadShows()
    }
    
    if (shows.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_search_results_found),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(shows, key = { it.id }) { show ->
                ShowListItem(
                    show = show,
                    onShowClick = { onShowClick(show.id) },
                    onStarClick = { viewModel.toggleStarred(show.id, !show.starred) },
                    onArchiveClick = { viewModel.toggleArchived(show.id, !show.archived) },
                    onWatchNextClick = { 
                        show.nextEpisodeId?.let { episodeId ->
                            viewModel.markEpisodeWatched(episodeId, true)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ShowListItem(
    show: Show,
    onShowClick: () -> Unit,
    onStarClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onWatchNextClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp)
            .clickable(onClick = onShowClick),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column {
            // Banner image with fixed height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Banner image
                if (show.bannerPath != null && show.bannerPath.isNotEmpty()) {
                    AsyncImage(
                        model = "https://image.tmdb.org/t/p/w1280/${show.bannerPath}",
                        contentDescription = show.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.blank_show_banner),
                        error = painterResource(R.drawable.blank_show_banner)
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.blank_show_banner),
                        contentDescription = show.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopStart)
                ) {
                    // Show name
                    Text(
                        text = show.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp, end = 80.dp),
                        color = Color.White,
                        fontSize = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                                blurRadius = 4f
                            )
                        )
                    )
                }
                
                // Star and Archive toggles
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    // Archive toggle
                    IconButton(
                        onClick = onArchiveClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = if (show.archived) {
                                painterResource(R.drawable.ic_show_archived)
                            } else {
                                painterResource(R.drawable.ic_show_unarchived)
                            },
                            contentDescription = if (show.archived) "Unarchive" else "Archive",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Star toggle
                    IconButton(
                        onClick = onStarClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (show.starred) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (show.starred) "Unstar" else "Star",
                            tint = if (show.starred) Color(0xFFFFD700) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Progress bar (thin line)
            if (show.totalCount > 0) {
                val progress = show.watchedCount.toFloat() / show.totalCount.toFloat()
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Next episode info on dark background
            if (show.nextEpisodeName != null && show.nextEpisodeSeasonNumber != null && show.nextEpisodeNumber != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "S%02dE%02d".format(show.nextEpisodeSeasonNumber, show.nextEpisodeNumber),
                            color = Color(0xFFF5F5F5),
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = show.nextEpisodeName,
                            color = Color(0xFFF5F5F5),
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Watch button
                    IconButton(
                        onClick = { onWatchNextClick() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Mark as watched",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
