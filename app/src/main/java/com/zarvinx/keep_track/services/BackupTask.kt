package com.zarvinx.keep_track.services

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.zarvinx.keep_track.AutoBackupHelper
import com.zarvinx.keep_track.FileUtilities
import com.zarvinx.keep_track.JsonBackup
import com.zarvinx.keep_track.KeepTrackApplication
import com.zarvinx.keep_track.R
import java.io.IOException
import java.util.concurrent.Callable

class BackupTask(
    private val destinationFileName: String,
    private val showToast: Boolean = true,
    private val maxBackupCount: Int? = null
) : Callable<Void?> {
    private val context: Context = KeepTrackApplication.instance.applicationContext

    override fun call(): Void? {
        Log.i(TAG, "Backing up library.")

        val dirUri = FileUtilities.get_backup_dir_uri(context)
        if (dirUri == null) {
            Log.w(TAG, "No backup directory set, skipping backup.")
            return null
        }

        val dir = DocumentFile.fromTreeUri(context, dirUri)
        if (dir == null || !dir.canWrite()) {
            Log.e(TAG, "Cannot write to backup directory.")
            return null
        }

        val newFile = dir.createFile("application/json", destinationFileName)
        if (newFile == null) {
            Log.e(TAG, "Failed to create backup file in directory.")
            return null
        }

        try {
            context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                outputStream.write(JsonBackup.exportToJson(context).toByteArray(Charsets.UTF_8))
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

            Log.i(TAG, "Library backed up successfully: '${newFile.uri}'.")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    companion object {
        private val TAG = BackupTask::class.java.name
    }
}
