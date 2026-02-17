package com.zarvinx.keep_track.db.room

import android.content.Context
import java.io.File

/**
 * Resolves which on-device DB filename should be opened by Room/backup/restore.
 */
object AppDatabaseFile {
    private const val NAME = "keep_track.db"

    @JvmStatic
    fun resolveDbName(): String {
        return NAME
    }

    @JvmStatic
    fun resolveDbPath(context: Context): File {
        return context.getDatabasePath(resolveDbName())
    }
}
