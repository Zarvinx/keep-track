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

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.redcoracle.episodes.services.AddShowTask
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.ui.AddShowPreviewArgs
import com.redcoracle.episodes.ui.AddShowPreviewScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShowPreviewScaffold(
    previewArgs: AddShowPreviewArgs?,
    onNavigateBack: () -> Unit,
    onAddShow: () -> Unit
) {
    val context = LocalContext.current
    if (previewArgs == null) {
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
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        AsyncTask().executeAsync(
                            AddShowTask(previewArgs.tmdbId, previewArgs.name, previewArgs.language),
                            onError = {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_adding_show, previewArgs.name),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                        onAddShow()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.menu_add_show))
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
