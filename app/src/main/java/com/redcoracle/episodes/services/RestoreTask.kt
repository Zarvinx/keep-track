package com.redcoracle.episodes.services

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.R
import com.redcoracle.episodes.db.DatabaseOpenHelper
import com.redcoracle.episodes.db.ShowsProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Callable

class RestoreTask(private val filename: String) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext

    override fun call(): Void? {
        if (!isExternalStorageWritable()) {
            return null
        }

        val backupFile = File(filename)
        val databaseFile = context.getDatabasePath(DatabaseOpenHelper.getDbName())
        
        try {
            FileInputStream(backupFile).channel.use { src ->
                FileOutputStream(databaseFile).channel.use { dest ->
                    dest.transferFrom(src, 0, src.size())
                }
            }
            
            Glide.get(context).clearDiskCache()
            
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(
                    context,
                    context.getString(R.string.restore_success_message),
                    Toast.LENGTH_LONG
                ).show()
            }
            
            Log.i(TAG, "Library restored successfully.")
        } catch (e: IOException) {
            Log.e(TAG, "Error restoring library: $e")
        } finally {
            ShowsProvider.reloadDatabase(context)
        }
        
        return null
    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED
    }

    companion object {
        private val TAG = RestoreTask::class.java.name
    }
}
