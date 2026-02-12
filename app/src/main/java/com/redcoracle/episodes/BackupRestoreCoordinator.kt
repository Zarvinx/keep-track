package com.redcoracle.episodes

import androidx.appcompat.app.AppCompatActivity
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.BackupTask
import com.redcoracle.episodes.services.RestoreTask

class BackupRestoreCoordinator(
    private val activity: AppCompatActivity? = null,
    private val showLegacyRestoreDialog: () -> Unit
) {
    fun backUp() {
        val retention = activity?.let {
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(it)
                .getInt(AutoBackupHelper.KEY_PREF_AUTO_BACKUP_RETENTION, 10)
                .coerceIn(1, 100)
        }

        AsyncTask().executeAsync(
            BackupTask(
                destinationFileName = FileUtilities.get_suggested_filename(),
                showToast = true,
                maxBackupCount = retention
            )
        )
    }

    fun restore() {
        showLegacyRestoreDialog()
    }

    fun onBackupSelected(backupFilename: String) {
        AsyncTask().executeAsync(RestoreTask(backupFilename))
    }
}
