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

package com.redcoracle.episodes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcoracle.episodes.ui.theme.EpisodesTheme

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            EpisodesTheme {
                AboutScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val websiteUrl = stringResource(R.string.about_website)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Description
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            
            // Website (clickable)
            val websiteText = buildAnnotatedString {
                pushStringAnnotation(tag = "URL", annotation = websiteUrl)
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                ) {
                    append(websiteUrl)
                }
                pop()
            }
            
            ClickableText(
                text = websiteText,
                onClick = { offset ->
                    websiteText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Divider()
            
            // License
            Text(
                text = stringResource(R.string.about_license),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
            
            // TMDB attribution
            Text(
                text = stringResource(R.string.about_tmdb),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start
            )
            
            Divider()
            
            // Version info
            Text(
                text = stringResource(
                    R.string.version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.GIT_COMMIT_ID
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
