/*
 * Copyright (C) 2014 Jamie Nicol <jamie@thenicols.net>
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun SelectBackupDialog(
    onBackupSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val backups = remember { getBackupFiles(context) }
    
    if (backups.isNotEmpty()) {
        BackupsListDialog(
            backups = backups,
            onBackupSelected = onBackupSelected,
            onDismiss = onDismiss
        )
    } else {
        NoBackupsDialog(
            context = context,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun BackupsListDialog(
    backups: List<File>,
    onBackupSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_dialog_title)) },
        text = {
            LazyColumn {
                items(backups) { backup ->
                    Text(
                        text = backup.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onBackupSelected(backup.path)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun NoBackupsDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    val directory = File(context.getExternalFilesDir(null), "episodes")
    val message = stringResource(R.string.restore_dialog_no_backups_message, directory)
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_dialog_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        }
    )
}

private fun getBackupFiles(context: Context): List<File> {
    val files = File(context.getExternalFilesDir(null), "episodes").listFiles()
    
    return files?.sortedByDescending { it.lastModified() } ?: emptyList()
}
