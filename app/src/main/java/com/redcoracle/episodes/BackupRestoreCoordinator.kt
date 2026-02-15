package com.redcoracle.episodes

import android.content.Context
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.BackupTask
import com.redcoracle.episodes.services.RestoreTask

/**
 * Coordinates manual backup and restore actions from settings UI.
 */
class BackupRestoreCoordinator(
    private val context: Context,
    private val showLegacyRestoreDialog: () -> Unit
) {
    /**
     * Starts an immediate backup task using configured retention.
     */
    fun backUp() {
        val retention = androidx.preference.PreferenceManager
            .getDefaultSharedPreferences(context)
            .getInt(AutoBackupHelper.KEY_PREF_AUTO_BACKUP_RETENTION, 10)
            .coerceIn(1, 100)

        AsyncTask().executeAsync(
            BackupTask(
                destinationFileName = FileUtilities.get_suggested_filename(),
                showToast = true,
                maxBackupCount = retention
            )
        )
    }

    /**
     * Requests UI to show backup selection before restore.
     */
    fun restore() {
        showLegacyRestoreDialog()
    }

    /**
     * Starts restore task for a user-selected backup file path.
     */
    fun onBackupSelected(backupFilename: String) {
        AsyncTask().executeAsync(RestoreTask(backupFilename))
    }
}
