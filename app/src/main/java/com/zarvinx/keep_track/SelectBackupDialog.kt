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

package com.zarvinx.keep_track

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
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SelectBackupDialog(
    onBackupSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val backups = remember { FileUtilities.get_backup_document_files(context) }

    if (backups.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.restore_dialog_title)) },
            text = {
                LazyColumn {
                    items(backups) { backup ->
                        Text(
                            text = formatBackupDisplayDate(backup),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onBackupSelected(backup.uri.toString())
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
    } else {
        val dirName = FileUtilities.get_backup_dir_display_name(context)
        val message = if (dirName != null) {
            stringResource(R.string.restore_dialog_no_backups_message, dirName)
        } else {
            stringResource(R.string.restore_dialog_no_folder_set)
        }
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
}

private fun formatBackupDisplayDate(file: DocumentFile): String {
    val date = Date(file.lastModified())
    val name = file.name ?: return SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(date)
    // Show the filename (without extension) as primary label alongside the date
    val displayName = name.removeSuffix(".json")
    return displayName
}
