package com.redcoracle.episodes.services

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.FileUtilities
import com.redcoracle.episodes.R
import com.redcoracle.episodes.db.DatabaseOpenHelper
import com.redcoracle.episodes.db.room.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Callable

class BackupTask(
    private val destinationFileName: String,
    private val showToast: Boolean = true,
    private val maxBackupCount: Int? = null
) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext

    override fun call(): Void? {
        Log.i(TAG, "Backing up library.")

        // Ensure Room-flushed pages are in the main DB file before copying.
        AppDatabase.checkpoint(context)

        val databaseFile = context.getDatabasePath(DatabaseOpenHelper.getDbName())
        val destinationDirectory = FileUtilities.get_backup_directory(context)

        val destinationFile = File(destinationDirectory, destinationFileName)
        
        try {
            FileInputStream(databaseFile).channel.use { src ->
                FileOutputStream(destinationFile).channel.use { dest ->
                    dest.transferFrom(src, 0, src.size())
                }
            }

            maxBackupCount?.let {
                FileUtilities.prune_old_backups(context, it)
            }

            if (showToast) {
                ContextCompat.getMainExecutor(context).execute {
                    Toast.makeText(
                        context,
                        String.format(context.getString(R.string.back_up_success_message), destinationFileName),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            
            Log.i(TAG, "Library backed up successfully: '${destinationFile.path}'.")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        return null
    }

    companion object {
        private val TAG = BackupTask::class.java.name
    }
}
