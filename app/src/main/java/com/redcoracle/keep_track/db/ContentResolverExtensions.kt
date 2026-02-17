/*
 * Copyright (C) 2026 Zarvinx
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

package com.redcoracle.keep_track.db

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Creates a Flow that observes changes to a ContentResolver query.
 * Automatically re-executes the query when the underlying data changes.
 */
fun ContentResolver.observeQuery(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Flow<Cursor?> = callbackFlow {
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // Query the database and emit the new cursor
            val cursor = query(uri, projection, selection, selectionArgs, sortOrder)
            trySend(cursor)
        }
    }
    
    // Register observer
    registerContentObserver(uri, true, observer)
    
    // Emit initial data
    val initialCursor = query(uri, projection, selection, selectionArgs, sortOrder)
    trySend(initialCursor)
    
    // Cleanup when Flow is cancelled
    awaitClose {
        unregisterContentObserver(observer)
    }
}
