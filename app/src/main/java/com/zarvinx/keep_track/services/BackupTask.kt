package com.zarvinx.keep_track.services

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.zarvinx.keep_track.KeepTrackApplication
import com.zarvinx.keep_track.FileUtilities
import com.zarvinx.keep_track.R
import com.zarvinx.keep_track.db.DatabaseOpenHelper
import com.zarvinx.keep_track.db.room.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Callable

/**
 * Background task that copies the app database to the backup directory.
 *
 * The task performs a Room checkpoint before copying and can optionally prune
 * old backups after a successful write.
 */
class BackupTask(
    private val destinationFileName: String,
    private val showToast: Boolean = true,
    private val maxBackupCount: Int? = null
) : Callable<Void?> {
    private val context: Context = KeepTrackApplication.instance.applicationContext

    /**
     * Executes the backup on a background thread.
     *
     * Errors are handled internally and surfaced via toast/logging.
     */
    override fun call(): Void? {
        Log.i(TAG, "Backing up library.")

        // Ensure Room-flushed pages are in the main DB file before copying.
        AppDatabase.checkpoint(context)

        val databaseFile = context.getDatabasePath(DatabaseOpenHelper.getDbName(context))
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
            Log.e(TAG, "Backup failed: database file not found.", e)
            showFailureToast(R.string.back_up_error_file_missing)
        } catch (e: SecurityException) {
            Log.e(TAG, "Backup failed: permission denied.", e)
            showFailureToast(R.string.back_up_error_permission)
        } catch (e: IOException) {
            Log.e(TAG, "Backup failed due to I/O error.", e)
            showFailureToast(mapIoErrorMessageRes(e))
        }
        
        return null
    }

    private fun showFailureToast(messageResId: Int) {
        ContextCompat.getMainExecutor(context).execute {
            Toast.makeText(
                context,
                context.getString(messageResId),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun mapIoErrorMessageRes(error: IOException): Int {
        val message = error.message?.lowercase(Locale.US).orEmpty()
        return if (message.contains("no space left") || message.contains("enospc")) {
            R.string.back_up_error_storage
        } else {
            R.string.back_up_error_message
        }
    }

    companion object {
        private val TAG = BackupTask::class.java.name
    }
}
