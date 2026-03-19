/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
 * Copyright (C) 2026 Zarvinx (Kotlin conversion)
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

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

object FileUtilities {
    const val KEY_PREF_BACKUP_DIR_URI = "pref_backup_dir_uri"

    fun get_suggested_filename(): String {
        val today = Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
        return "keep_track_${formatter.format(today)}.json"
    }

    fun uri_to_filename(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
        return null
    }

    fun get_backup_dir_uri(context: Context): Uri? {
        val uriString = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_PREF_BACKUP_DIR_URI, null) ?: return null
        return Uri.parse(uriString)
    }

    fun get_backup_dir_display_name(context: Context): String? {
        val uri = get_backup_dir_uri(context) ?: return null
        val lastSegment = uri.lastPathSegment ?: return uri.toString()
        return lastSegment.substringAfter(':')
    }

    fun get_backup_document_files(context: Context): List<DocumentFile> {
        val dirUri = get_backup_dir_uri(context) ?: return emptyList()
        val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return emptyList()
        return dir.listFiles()
            .filter { it.name?.endsWith(".json") == true }
            .sortedByDescending { it.lastModified() }
    }

    fun prune_old_backups(context: Context, maxBackupCount: Int) {
        val keep = maxBackupCount.coerceIn(1, 100)
        val backups = get_backup_document_files(context)
        if (backups.size <= keep) return
        backups.drop(keep).forEach { it.delete() }
    }
}
