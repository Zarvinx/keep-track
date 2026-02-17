package com.zarvinx.keep_track.services

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.zarvinx.keep_track.KeepTrackApplication
import com.zarvinx.keep_track.R
import com.zarvinx.keep_track.db.room.AppDatabase
import com.zarvinx.keep_track.db.room.AppDatabaseFile
import java.io.File
import java.io.FileNotFoundException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Callable

/**
 * Background task that restores the app database from a selected backup file.
 *
 * The Room singleton is closed before replacement and reloaded afterwards.
 */
class RestoreTask(private val filename: String) : Callable<Void?> {
    private val context: Context = KeepTrackApplication.instance.applicationContext

    /**
     * Executes restore work on a background thread and posts result toasts.
     */
    override fun call(): Void? {
        val backupFile = File(filename)
        val databaseFile = AppDatabaseFile.resolveDbPath(context)
        
        try {
            // Close active Room connection before replacing the DB file.
            AppDatabase.closeInstance()

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
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Error restoring library: backup file not found.", e)
            showFailureToast(R.string.restore_error_file_missing)
        } catch (e: SecurityException) {
            Log.e(TAG, "Error restoring library: permission denied.", e)
            showFailureToast(R.string.restore_error_permission)
        } catch (e: IOException) {
            Log.e(TAG, "Error restoring library due to I/O error.", e)
            showFailureToast(mapIoErrorMessageRes(e))
        } finally {
            // Ensure stale Room handles are cleared after replacement.
            AppDatabase.closeInstance()
        }
        
        return null
    }

    companion object {
        private val TAG = RestoreTask::class.java.name
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
            R.string.restore_error_storage
        } else {
            R.string.restore_error_message
        }
    }
}
