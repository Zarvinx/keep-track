package com.zarvinx.keep_track.db.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS seasons (" +
                    "show_id INTEGER NOT NULL, " +
                    "season_number INTEGER NOT NULL, " +
                    "name TEXT, " +
                    "PRIMARY KEY(show_id, season_number))"
            )
        }
    }

    val ALL = arrayOf(MIGRATION_14_15)
}
