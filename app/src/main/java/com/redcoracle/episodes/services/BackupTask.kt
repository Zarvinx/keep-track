package com.redcoracle.episodes.services

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.R
import com.redcoracle.episodes.db.DatabaseOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Callable

class BackupTask(private val destinationFileName: String) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext

    override fun call(): Void? {
        Log.i(TAG, "Backing up library.")
        if (!isExternalStorageReadable()) {
            Log.i(TAG, "Storage is not readable.")
            return null
        }

        val databaseFile = context.getDatabasePath(DatabaseOpenHelper.getDbName())
        val destinationDirectory = File(context.getExternalFilesDir(null), "episodes")
        
        if (!destinationDirectory.mkdirs()) {
            Log.e(TAG, "Error creating backup directory '${destinationDirectory.path}'.")
        }

        val destinationFile = File(destinationDirectory, destinationFileName)
        
        try {
            FileInputStream(databaseFile).channel.use { src ->
                FileOutputStream(destinationFile).channel.use { dest ->
                    dest.transferFrom(src, 0, src.size())
                }
            }
            
            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(
                    context,
                    String.format(context.getString(R.string.back_up_success_message), destinationFileName),
                    Toast.LENGTH_LONG
                ).show()
            }
            
            Log.i(TAG, "Library backed up successfully: '${destinationFile.path}'.")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        return null
    }

    private fun isExternalStorageReadable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }

    companion object {
        private val TAG = BackupTask::class.java.name
    }
}
