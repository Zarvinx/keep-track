package com.redcoracle.episodes

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.redcoracle.episodes.db.DatabaseOpenHelper
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.BackupTask
import com.redcoracle.episodes.services.RestoreTask
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackupRestoreCoordinator(
    private val activity: AppCompatActivity,
    private val hasStoragePermission: () -> Boolean,
    private val showLegacyRestoreDialog: () -> Unit
) {
    companion object {
        private const val WRITE_REQUEST_CODE = 0
        private const val READ_REQUEST_CODE = 1
    }

    fun backUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/x-sqlite3"
                putExtra(Intent.EXTRA_TITLE, FileUtilities.get_suggested_filename())
            }
            activity.startActivityForResult(intent, WRITE_REQUEST_CODE)
        } else if (hasStoragePermission()) {
            AsyncTask().executeAsync(BackupTask(FileUtilities.get_suggested_filename()))
        }
    }

    fun restore() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/x-sqlite3"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream"))
            }
            activity.startActivityForResult(intent, READ_REQUEST_CODE)
        } else if (hasStoragePermission()) {
            showLegacyRestoreDialog()
        }
    }

    fun onBackupSelected(backupFilename: String) {
        AsyncTask().executeAsync(RestoreTask(backupFilename))
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != WRITE_REQUEST_CODE && requestCode != READ_REQUEST_CODE) {
            return false
        }

        if (resultCode != Activity.RESULT_OK) {
            return true
        }

        try {
            if (requestCode == WRITE_REQUEST_CODE) {
                val uri = data?.data ?: return true
                FileUtilities.copy_file(
                    FileInputStream(activity.getDatabasePath(DatabaseOpenHelper.getDbName())).channel,
                    FileOutputStream(activity.contentResolver.openFileDescriptor(uri, "w")?.fileDescriptor).channel
                )
                Toast.makeText(
                    activity,
                    String.format(
                        activity.getString(R.string.back_up_success_message),
                        FileUtilities.uri_to_filename(activity, uri)
                    ),
                    Toast.LENGTH_LONG
                ).show()
                return true
            }

            val uri = data?.data ?: return true
            FileUtilities.copy_file(
                FileInputStream(activity.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor).channel,
                FileOutputStream(activity.getDatabasePath(DatabaseOpenHelper.getDbName())).channel
            )
            ShowsProvider.reloadDatabase(activity)
            CoroutineScope(Dispatchers.IO).launch {
                Glide.get(activity.applicationContext).clearDiskCache()
            }
            Toast.makeText(activity, activity.getString(R.string.restore_success_message), Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        return true
    }
}

