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

package com.redcoracle.keep_track.ui

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
import com.redcoracle.keep_track.R
import java.text.DateFormat
import java.util.*

@Composable
fun ShowOverviewScreen(showId: Int) {
    val viewModel: ShowViewModel = hiltViewModel()
    LaunchedEffect(showId) {
        viewModel.initialize(showId)
    }
    
    val showDetails by viewModel.showDetails.collectAsState()
    val scrollState = rememberScrollState()
    
    if (showDetails == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview text
            showDetails?.overview?.let { overview ->
                if (overview.isNotBlank()) {
                    Text(
                        text = overview.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // First aired date
            showDetails?.firstAired?.let { firstAiredTimestamp ->
                val firstAired = Date(firstAiredTimestamp * 1000)
                val dateFormat = DateFormat.getDateInstance()
                val firstAiredText = stringResource(R.string.first_aired, dateFormat.format(firstAired))
                
                Text(
                    text = firstAiredText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
