package com.redcoracle.keep_track

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import com.redcoracle.keep_track.services.AsyncTask
import com.redcoracle.keep_track.services.BackupTask

/**
 * Schedules and runs periodic local database backups.
 *
 * Uses shared preferences for schedule/retention settings and AlarmManager
 * for deferred execution.
 */
class AutoBackupHelper private constructor(
    private val context: Context
) : SharedPreferences.OnSharedPreferenceChangeListener {

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        ensureDefaults()
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    companion object {
        private val TAG = AutoBackupHelper::class.java.name

        const val KEY_PREF_AUTO_BACKUP_ENABLED = "pref_auto_backup_enabled"
        const val KEY_PREF_AUTO_BACKUP_PERIOD = "pref_auto_backup_period"
        const val KEY_PREF_AUTO_BACKUP_RETENTION = "pref_auto_backup_retention"
        private const val KEY_LAST_AUTO_BACKUP_TIME = "last_auto_backup_time"

        const val PERIOD_DAILY = "daily"
        const val PERIOD_WEEKLY = "weekly"
        const val PERIOD_MONTHLY = "monthly"

        @Volatile
        private var instance: AutoBackupHelper? = null

        /**
         * Returns singleton helper instance scoped to application context.
         */
        @JvmStatic
        fun getInstance(context: Context): AutoBackupHelper {
            return instance ?: synchronized(this) {
                instance ?: AutoBackupHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Reacts to scheduling-related preference changes.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEY_PREF_AUTO_BACKUP_ENABLED,
            KEY_PREF_AUTO_BACKUP_PERIOD,
            KEY_PREF_AUTO_BACKUP_RETENTION -> rescheduleAlarm()
        }
    }

    private fun ensureDefaults() {
        val editor = preferences.edit()
        var changed = false

        if (!preferences.contains(KEY_PREF_AUTO_BACKUP_ENABLED)) {
            editor.putBoolean(KEY_PREF_AUTO_BACKUP_ENABLED, false)
            changed = true
        }
        if (!preferences.contains(KEY_PREF_AUTO_BACKUP_PERIOD)) {
            editor.putString(KEY_PREF_AUTO_BACKUP_PERIOD, PERIOD_WEEKLY)
            changed = true
        }
        if (!preferences.contains(KEY_PREF_AUTO_BACKUP_RETENTION)) {
            editor.putInt(KEY_PREF_AUTO_BACKUP_RETENTION, 10)
            changed = true
        }

        if (changed) {
            editor.apply()
        }
    }

    /**
     * Recomputes and schedules the next auto-backup alarm.
     */
    fun rescheduleAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = getPendingIntent()

        if (!preferences.getBoolean(KEY_PREF_AUTO_BACKUP_ENABLED, false)) {
            Log.i(TAG, "Cancelling auto backup alarm.")
            alarmManager.cancel(pendingIntent)
            return
        }

        val interval = getIntervalMillis()
        val lastRun = preferences.getLong(KEY_LAST_AUTO_BACKUP_TIME, 0L)
        val nextRun = if (lastRun == 0L) {
            System.currentTimeMillis() + interval
        } else {
            lastRun + interval
        }.coerceAtLeast(System.currentTimeMillis() + 60_000L)

        Log.i(TAG, "Scheduling auto backup alarm for $nextRun.")
        // Non-exact alarms are sufficient for daily/weekly/monthly backups and avoid
        // Android 12+ exact-alarm permission requirements.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRun, pendingIntent)
    }

    /**
     * Runs a backup immediately and updates last-run schedule metadata.
     */
    fun runBackupNow() {
        val retention = preferences.getInt(KEY_PREF_AUTO_BACKUP_RETENTION, 10).coerceIn(1, 100)
        AsyncTask().executeAsync(
            BackupTask(
                destinationFileName = FileUtilities.get_suggested_filename(),
                showToast = false,
                maxBackupCount = retention
            )
        )
        preferences.edit().putLong(KEY_LAST_AUTO_BACKUP_TIME, System.currentTimeMillis()).apply()
        rescheduleAlarm()
    }

    /**
     * Applies retention policy immediately by deleting oldest backups.
     */
    fun pruneBackupsNow() {
        val retention = preferences.getInt(KEY_PREF_AUTO_BACKUP_RETENTION, 10).coerceIn(1, 100)
        FileUtilities.prune_old_backups(context, retention)
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, Receiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 1, intent, flags)
    }

    private fun getIntervalMillis(): Long {
        return when (preferences.getString(KEY_PREF_AUTO_BACKUP_PERIOD, PERIOD_WEEKLY)) {
            PERIOD_DAILY -> 24L * 60L * 60L * 1000L
            PERIOD_MONTHLY -> 30L * 24L * 60L * 60L * 1000L
            else -> 7L * 24L * 60L * 60L * 1000L
        }
    }

    /**
     * Broadcast receiver entrypoint triggered by the scheduled backup alarm.
     */
    class Receiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            getInstance(context.applicationContext).runBackupNow()
        }
    }
}
