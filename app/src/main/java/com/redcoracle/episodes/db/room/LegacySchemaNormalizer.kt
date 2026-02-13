package com.redcoracle.episodes.db.room

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.redcoracle.episodes.db.DatabaseOpenHelper

internal object LegacySchemaNormalizer {
    fun normalizeEpisodesTable(context: Context) {
        val databaseFile = context.getDatabasePath(DatabaseOpenHelper.getDbName())
        if (!databaseFile.exists()) {
            return
        }

        val db = SQLiteDatabase.openDatabase(
            databaseFile.path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )

        db.use { sqliteDb ->
            val createSql = sqliteDb.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='episodes'",
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: return

            val needsTypeNormalization = hasLegacyTypeDeclarations(createSql)
            val needsRowIdNotNullNormalization = hasImplicitNullablePrimaryKey(createSql)
            val normalizedAlready = !needsTypeNormalization && !needsRowIdNotNullNormalization
            if (normalizedAlready) {
                return
            }

            sqliteDb.beginTransaction()
            try {
                sqliteDb.execSQL(
                    """
                    CREATE TABLE episodes_room_normalized (
                        _id INTEGER NOT NULL PRIMARY KEY,
                        tvdb_id INTEGER UNIQUE,
                        tmdb_id INTEGER UNIQUE,
                        imdb_id TEXT UNIQUE,
                        show_id INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        language TEXT,
                        overview TEXT,
                        episode_number INTEGER,
                        season_number INTEGER,
                        first_aired INTEGER,
                        watched INTEGER
                    )
                    """.trimIndent()
                )

                sqliteDb.execSQL(
                    """
                    INSERT INTO episodes_room_normalized
                    (_id, tvdb_id, tmdb_id, imdb_id, show_id, name, language, overview, episode_number, season_number, first_aired, watched)
                    SELECT
                    _id,
                    tvdb_id,
                    tmdb_id,
                    imdb_id,
                    show_id,
                    name,
                    language,
                    overview,
                    episode_number,
                    season_number,
                    CASE WHEN first_aired IS NULL THEN NULL ELSE CAST(first_aired AS INTEGER) END,
                    CASE WHEN watched IS NULL THEN NULL ELSE CAST(watched AS INTEGER) END
                    FROM episodes
                    """.trimIndent()
                )

                sqliteDb.execSQL("DROP TABLE episodes")
                sqliteDb.execSQL("ALTER TABLE episodes_room_normalized RENAME TO episodes")
                sqliteDb.setTransactionSuccessful()
            } finally {
                sqliteDb.endTransaction()
            }
        }
    }

    private fun hasLegacyTypeDeclarations(createSql: String): Boolean {
        val sql = createSql.uppercase()
        return sql.contains("VARCHAR(200)") || sql.contains(" DATE") || sql.contains("BOOLEAN")
    }

    private fun hasImplicitNullablePrimaryKey(createSql: String): Boolean {
        val sql = createSql.uppercase().replace('\n', ' ')
        val hasPrimaryKey = sql.contains("_ID INTEGER PRIMARY KEY")
        val hasExplicitNotNullPrimaryKey = sql.contains("_ID INTEGER NOT NULL PRIMARY KEY")
        return hasPrimaryKey && !hasExplicitNotNullPrimaryKey
    }
}
