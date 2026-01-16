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

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.redcoracle.episodes.ui.AddShowSearchScreen
import com.redcoracle.episodes.ui.AddShowSearchViewModel
import com.redcoracle.episodes.ui.AddShowSearchViewModelFactory
import com.redcoracle.episodes.ui.theme.EpisodesTheme

class AddShowSearchActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val query = intent.getStringExtra("query") ?: ""
        
        setContent {
            EpisodesTheme {
                AddShowSearchScaffold(
                    query = query,
                    onNavigateBack = { navigateToMain() },
                    onShowClick = { index -> navigateToPreview(index) }
                )
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToPreview(resultIndex: Int) {
        val intent = Intent(this, AddShowPreviewActivity::class.java)
        intent.putExtra("searchResultIndex", resultIndex)
        startActivity(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShowSearchScaffold(
    query: String,
    onNavigateBack: () -> Unit,
    onShowClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(query) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            AddShowSearchScreen(
                query = query,
                onShowClick = onShowClick
            )
        }
    }
}
