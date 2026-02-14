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

package com.redcoracle.episodes

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.redcoracle.episodes.services.AddShowTask
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.ui.AddShowPreviewArgs
import com.redcoracle.episodes.ui.AddShowPreviewScreen
import com.redcoracle.episodes.ui.theme.EpisodesTheme

class AddShowPreviewActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PREVIEW_ARGS = "preview_args"

        fun createIntent(context: Context, args: AddShowPreviewArgs): Intent {
            return Intent(context, AddShowPreviewActivity::class.java).putExtra(EXTRA_PREVIEW_ARGS, args)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val previewArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_PREVIEW_ARGS, AddShowPreviewArgs::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PREVIEW_ARGS)
        }
        
        setContent {
            EpisodesTheme {
                AddShowPreviewScaffold(
                    previewArgs = previewArgs,
                    onNavigateBack = { finish() },
                    onAddShow = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShowPreviewScaffold(
    previewArgs: AddShowPreviewArgs?,
    onNavigateBack: () -> Unit,
    onAddShow: () -> Unit
) {
    if (previewArgs == null) {
        // Data was destroyed, navigate back
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(previewArgs.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Add the show
                        AsyncTask().executeAsync(
                            AddShowTask(previewArgs.tmdbId, previewArgs.name, previewArgs.language)
                        )
                        onAddShow()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add show")
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
            AddShowPreviewScreen(previewArgs = previewArgs)
        }
    }
}
