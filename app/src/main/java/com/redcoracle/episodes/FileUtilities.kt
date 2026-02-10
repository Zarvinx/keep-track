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

package com.redcoracle.episodes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*

object FileUtilities {
    fun get_suggested_filename(): String {
        val today = Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.getDefault())
        return "episodes_${formatter.format(today)}.db"
    }

    fun uri_to_filename(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            return cursor.getString(nameIndex)
        }
        return null
    }

    fun copy_file(source: FileChannel, destination: FileChannel) {
        try {
            destination.transferFrom(source, 0, source.size())
            source.close()
            destination.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
