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

import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.preference.PreferenceManager
import coil.compose.AsyncImage
import com.redcoracle.episodes.Preferences
import com.redcoracle.episodes.R
import com.redcoracle.episodes.ui.theme.AppShadows
import com.redcoracle.episodes.ui.theme.BackgroundGradientOption
import com.redcoracle.episodes.ui.theme.findBackgroundGradientOption

import java.util.concurrent.TimeUnit

@Composable
fun ShowsListScreen(
    viewModel: ShowsViewModel = hiltViewModel(),
    onShowClick: (Int) -> Unit
) {
    val shows by viewModel.shows.collectAsState()
    val listState = rememberLazyListState()
    var previousShowCount by remember { mutableStateOf(shows.size) }
    
    // Scroll to top when a new show is added
    LaunchedEffect(shows.size) {
        if (shows.size > previousShowCount && listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
        previousShowCount = shows.size
    }
    
    val selectedGradient = rememberSelectedBackgroundGradient()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(selectedGradient.startColor, selectedGradient.endColor),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        if (shows.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_search_results_found),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = shows, 
                    key = { it.id },
                    contentType = { "show_item" }
                ) { show ->
                    ShowListItem(
                        show = show,
                        onShowClick = onShowClick,
                        onStarClick = remember { { id -> viewModel.toggleStarred(id, !show.starred) } },
                        onArchiveClick = remember { { id -> viewModel.toggleArchived(id, !show.archived) } },
                        onWatchNextClick = remember { { episodeId: Int -> viewModel.markEpisodeWatched(episodeId, true) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberSelectedBackgroundGradient(): BackgroundGradientOption {
    val context = LocalContext.current
    val prefs = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    var selectedId by remember {
        mutableStateOf(
            prefs.getString(
                Preferences.KEY_PREF_BACKGROUND_GRADIENT,
                Preferences.BACKGROUND_GRADIENT_MIST_BLUE
            ) ?: Preferences.BACKGROUND_GRADIENT_MIST_BLUE
        )
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == Preferences.KEY_PREF_BACKGROUND_GRADIENT) {
                selectedId = sharedPrefs.getString(
                    Preferences.KEY_PREF_BACKGROUND_GRADIENT,
                    Preferences.BACKGROUND_GRADIENT_MIST_BLUE
                ) ?: Preferences.BACKGROUND_GRADIENT_MIST_BLUE
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return findBackgroundGradientOption(selectedId)
}

@Composable
fun ShowListItem(
    show: Show,
    onShowClick: (Int) -> Unit,
    onStarClick: (Int) -> Unit,
    onArchiveClick: (Int) -> Unit,
    onWatchNextClick: (Int) -> Unit
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val episodeBarBackgroundColor = if (isLightTheme) Color(0xFFE6E6E6) else Color(0xFF2A2A2A)
    val episodeBarTextColor = if (isLightTheme) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)

    val imageUrl = remember(show.bannerPath) {
        show.bannerPath?.takeIf { it.isNotEmpty() }?.let { "https://image.tmdb.org/t/p/w500/$it" }
    }
    val progress = remember(show.watchedCount, show.totalCount) {
        if (show.totalCount > 0) show.watchedCount.toFloat() / show.totalCount.toFloat() else 0f
    }
    val episodeCode = remember(show.nextEpisodeSeasonNumber, show.nextEpisodeNumber) {
        if (show.nextEpisodeSeasonNumber != null && show.nextEpisodeNumber != null) {
            "S%02dE%02d".format(show.nextEpisodeSeasonNumber, show.nextEpisodeNumber)
        } else null
    }
    val episodeStatus = remember(show.nextEpisodeAirDate) {
        calculateEpisodeStatus(show)
    }
    
    // Episode is watchable only if it has an air date AND it has already aired
    val isWatchable = remember(show.nextEpisodeAirDate) {
        show.nextEpisodeAirDate?.let { it <= System.currentTimeMillis() } ?: false
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onShowClick(show.id) },
        tonalElevation = 1.dp,
        shadowElevation = 4.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        Column {
            // Banner image with fixed height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                // Banner image
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
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
                    // Show name with marquee effect
                    MarqueeText(
                        text = show.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 4.dp, end = 80.dp),
                        color = Color.White,
                        fontSize = 22.sp,
                        style = MaterialTheme.typography.titleLarge,
                        shadow = AppShadows.TextOnImage,
                        maxLines = 1
                    )
                }
                
                // Star and Archive toggles on dark background in corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(
                            color = Color.DarkGray.copy(alpha = 0.7f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 8.dp,
                                bottomStart = 8.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .padding(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Archive toggle
                        IconButton(
                            onClick = { onArchiveClick(show.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (show.archived) Icons.Filled.Archive else Icons.Filled.Unarchive,
                                contentDescription = if (show.archived) "Unarchive" else "Archive",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Star toggle
                        IconButton(
                            onClick = { onStarClick(show.id) },
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
                
                // Status text overlay at bottom of banner (for upcoming episodes)
                if (episodeStatus != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(Color(0xFF000000).copy(alpha = 0.75f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = episodeStatus.text,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Progress bar (thin line)
            if (show.totalCount > 0) {
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
            if (show.nextEpisodeName != null && episodeCode != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(episodeBarBackgroundColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = episodeCode,
                            color = episodeBarTextColor,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        MarqueeText(
                            text = show.nextEpisodeName,
                            modifier = Modifier.weight(1f),
                            color = episodeBarTextColor,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }
                    
                    // Watch button (disabled if episode hasn't aired yet or has no air date)
                    IconButton(
                        onClick = { show.nextEpisodeId?.let(onWatchNextClick) },
                        modifier = Modifier.size(36.dp),
                        enabled = isWatchable
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = if (isWatchable) "Mark as watched" else "Not yet available",
                            tint = if (isWatchable) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0xFF555555)
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(episodeBarBackgroundColor)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    StatusText(
                        status = show.status,
                        textColor = episodeBarTextColor
                    )
                }
            }
        }
    }
}

/**
 * Data class to hold episode status information for upcoming episodes
 */
private data class EpisodeStatus(
    val text: String
)

/**
 * Calculate status text for upcoming episodes only
 * Returns null for aired episodes, missing data, or caught-up shows
 */
private fun calculateEpisodeStatus(show: Show): EpisodeStatus? {
    val airDate = show.nextEpisodeAirDate ?: return null
    val diffMillis = airDate - System.currentTimeMillis()
    
    // Only show status for episodes that haven't aired yet
    if (diffMillis <= 0) return null
    
    val daysUntil = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return when {
        daysUntil == 0L -> EpisodeStatus(
            text = "Next episode airs today"
        )
        daysUntil == 1L -> EpisodeStatus(
            text = "Next episode airs tomorrow"
        )
        daysUntil < 7 -> EpisodeStatus(
            text = "Next episode airs in $daysUntil days"
        )
        daysUntil < 30 -> {
            val weeks = daysUntil / 7
            EpisodeStatus(
                text = "Next episode airs in ${weeks} week${if (weeks > 1) "s" else ""}"
            )
        }
        else -> {
            val months = daysUntil / 30
            EpisodeStatus(
                text = "Next episode airs in ${months} month${if (months > 1) "s" else ""}"
            )
        }
    }
}
