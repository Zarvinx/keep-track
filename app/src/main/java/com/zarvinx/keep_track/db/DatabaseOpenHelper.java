/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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

package com.zarvinx.keep_track.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseOpenHelper";
    private static final String NAME = "keep_track.db";
    private static final String LEGACY_NAME = "episodes.db";
    private static final int version = 10;

    DatabaseOpenHelper(Context context) {
        super(context, getDbName(context), null, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "creating database");
        ShowsTable.onCreate(db);
        EpisodesTable.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, String.format("upgrading database from version %d to %d", oldVersion, newVersion));
        ShowsTable.onUpgrade(db, oldVersion, newVersion);
        EpisodesTable.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        Log.d(TAG, "opening database.");
    }

    public static String getDbName(Context context) {
        if (context.getDatabasePath(LEGACY_NAME).exists()) {
            return LEGACY_NAME;
        }
        return NAME;
    }
}
