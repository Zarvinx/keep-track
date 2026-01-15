package com.redcoracle.episodes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redcoracle.episodes.R
import java.text.DateFormat
import java.util.*

@Composable
fun ShowOverviewScreen(showId: Int) {
    val context = LocalContext.current
    val viewModel: ShowViewModel = viewModel(
        factory = ShowViewModelFactory(
            application = context.applicationContext as android.app.Application,
            showId = showId
        ),
        key = "show_$showId"
    )
    
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
