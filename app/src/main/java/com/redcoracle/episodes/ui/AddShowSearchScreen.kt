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

package com.redcoracle.episodes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.redcoracle.episodes.R
import com.redcoracle.episodes.tvdb.Show
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AddShowSearchScreen(
    query: String,
    onShowClick: (Int) -> Unit,
    viewModel: AddShowSearchViewModel
) {
    val searchState by viewModel.searchState.collectAsState()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = searchState) {
            is SearchState.Idle -> {
                // Initial state
            }
            
            is SearchState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            is SearchState.Success -> {
                if (state.results.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_search_results_found),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = state.results,
                            key = { _, show -> show.id }
                        ) { index, show ->
                            ShowSearchResultItem(
                                show = show,
                                onClick = {
                                    viewModel.selectShow(show)
                                    onShowClick(index)
                                }
                            )
                        }
                    }
                }
            }
            
            is SearchState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${state.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.searchShows() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

@Composable
fun ShowSearchResultItem(
    show: Show,
    onClick: () -> Unit
) {
    val releaseYear = show.firstAired?.let { date ->
        SimpleDateFormat("yyyy", Locale.getDefault()).format(date)
    }
    
    val posterUrl = show.posterPath?.takeIf { it.isNotEmpty() }?.let {
        "https://image.tmdb.org/t/p/w185$it"
    }
    
    val displayName = if (releaseYear != null) {
        "${show.name} ($releaseYear)"
    } else {
        show.name
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
            // Show name with year
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Poster thumbnail on the right
            AsyncImage(
                model = posterUrl,
                contentDescription = show.name,
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp)
                    .clip(MaterialTheme.shapes.small),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.blank_show_banner),
                error = painterResource(R.drawable.blank_show_banner)
            )
        }
    }
}
