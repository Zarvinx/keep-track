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
import java.io.File
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

/**
 * File helper utilities for backup file naming, lookup, copying, and retention.
 */
object FileUtilities {
    /**
     * Builds a timestamped backup filename in app convention.
     */
    fun get_suggested_filename(): String {
        val today = Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
        return "keep_track_${formatter.format(today)}.db"
    }

    /**
     * Resolves a display filename from a content URI when available.
     */
    fun uri_to_filename(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
        return null
    }

    /**
     * Copies all bytes from [source] channel to [destination] and closes both channels.
     */
    fun copy_file(source: FileChannel, destination: FileChannel) {
        try {
            destination.transferFrom(source, 0, source.size())
            source.close()
            destination.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the app-local backup directory, creating it when missing.
     */
    fun get_backup_directory(context: Context): File {
        val backupDirectory = File(context.filesDir, "backups")
        if (!backupDirectory.exists()) {
            backupDirectory.mkdirs()
        }
        return backupDirectory
    }

    /**
     * Returns backup files sorted newest-first.
     */
    fun get_backup_files(context: Context): List<File> {
        val files = get_backup_directory(context).listFiles()
        return files?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Deletes oldest backups so at most [maxBackupCount] files remain.
     */
    fun prune_old_backups(context: Context, maxBackupCount: Int) {
        val keep = maxBackupCount.coerceIn(1, 100)
        val backups = get_backup_files(context)
        if (backups.size <= keep) {
            return
        }

        backups.drop(keep).forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
