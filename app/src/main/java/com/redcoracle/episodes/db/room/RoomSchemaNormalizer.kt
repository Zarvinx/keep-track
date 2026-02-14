package com.redcoracle.episodes.db.room

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.redcoracle.episodes.db.DatabaseOpenHelper

/**
 * Normalizes legacy SQLite type declarations that Room schema validation rejects.
 */
object RoomSchemaNormalizer {
    private const val TAG = "RoomSchemaNormalizer"

    fun normalizeIfNeeded(context: Context) {
        runCatching {
            val dbPath = context.getDatabasePath(DatabaseOpenHelper.getDbName())
            if (!dbPath.exists()) return

            val db = SQLiteDatabase.openDatabase(dbPath.path, null, SQLiteDatabase.OPEN_READWRITE)
            db.use {
                if (needsShowsNormalization(it)) {
                    Log.i(TAG, "Normalizing shows table schema for Room compatibility")
                    normalizeShowsTable(it)
                }
            }
        }.onFailure { error ->
            Log.e(TAG, "Schema normalization failed", error)
        }
    }

    private fun needsShowsNormalization(db: SQLiteDatabase): Boolean {
        val columnTypes = mutableMapOf<String, String>()
        db.rawQuery("PRAGMA table_info(shows)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            while (cursor.moveToNext()) {
                val columnName = cursor.getString(nameIndex)
                val columnType = cursor.getString(typeIndex)?.uppercase() ?: ""
                columnTypes[columnName] = columnType
            }
        }

        if (columnTypes.isEmpty()) return false

        return columnTypes["imdb_id"] != "TEXT" ||
            columnTypes["first_aired"] != "INTEGER" ||
            columnTypes["starred"] != "INTEGER" ||
            columnTypes["archived"] != "INTEGER"
    }

    private fun normalizeShowsTable(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            db.execSQL(
                """
                CREATE TABLE shows_room_normalized (
                    _id INTEGER PRIMARY KEY,
                    tvdb_id INTEGER UNIQUE,
                    tmdb_id INTEGER UNIQUE,
                    imdb_id TEXT UNIQUE,
                    name TEXT NOT NULL,
                    language TEXT,
                    overview TEXT,
                    first_aired INTEGER,
                    starred INTEGER DEFAULT 0,
                    archived INTEGER DEFAULT 0,
                    banner_path TEXT,
                    fanart_path TEXT,
                    poster_path TEXT,
                    notes TEXT,
                    status TEXT
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                INSERT INTO shows_room_normalized (
                    _id, tvdb_id, tmdb_id, imdb_id, name, language, overview,
                    first_aired, starred, archived, banner_path, fanart_path,
                    poster_path, notes, status
                )
                SELECT
                    _id,
                    tvdb_id,
                    tmdb_id,
                    imdb_id,
                    name,
                    language,
                    overview,
                    CASE
                        WHEN first_aired IS NULL THEN NULL
                        ELSE CAST(first_aired AS INTEGER)
                    END,
                    CASE
                        WHEN starred IS NULL THEN 0
                        WHEN starred IN (1, '1', 'true', 'TRUE') THEN 1
                        ELSE 0
                    END,
                    CASE
                        WHEN archived IS NULL THEN 0
                        WHEN archived IN (1, '1', 'true', 'TRUE') THEN 1
                        ELSE 0
                    END,
                    banner_path,
                    fanart_path,
                    poster_path,
                    notes,
                    status
                FROM shows
                """.trimIndent()
            )

            db.execSQL("DROP TABLE shows")
            db.execSQL("ALTER TABLE shows_room_normalized RENAME TO shows")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
