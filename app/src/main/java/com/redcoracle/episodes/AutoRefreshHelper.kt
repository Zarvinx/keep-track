/*
 * Copyright (C) 2014 Jamie Nicol <jamie@thenicols.net>
 * Copyright (C) 2026 Zarvinx (Kotlin conversion)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redcoracle.episodes

import android.app.AlarmManager
import android.app.IntentService
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.core.net.ConnectivityManagerCompat
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.RefreshAllShowsTask

class AutoRefreshHelper private constructor(
    private val context: Context
) : SharedPreferences.OnSharedPreferenceChangeListener {
    
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        preferences.registerOnSharedPreferenceChangeListener(this)
    }

    companion object {
        private val TAG = AutoRefreshHelper::class.java.name

        private const val KEY_PREF_AUTO_REFRESH_ENABLED = "pref_auto_refresh_enabled"
        private const val KEY_PREF_AUTO_REFRESH_PERIOD = "pref_auto_refresh_period"
        private const val KEY_PREF_AUTO_REFRESH_WIFI_ONLY = "pref_auto_refresh_wifi_only"
        private const val KEY_LAST_AUTO_REFRESH_TIME = "last_auto_refresh_time"
        private const val KEY_PREF_CONFIRMED_BACKUP = "pref_confirmed_backup"

        @Volatile
        private var instance: AutoRefreshHelper? = null

        @JvmStatic
        fun getInstance(context: Context): AutoRefreshHelper {
            return instance ?: synchronized(this) {
                instance ?: AutoRefreshHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            KEY_PREF_AUTO_REFRESH_ENABLED,
            KEY_PREF_AUTO_REFRESH_PERIOD,
            KEY_PREF_AUTO_REFRESH_WIFI_ONLY -> rescheduleAlarm()
        }
    }

    private fun getAutoRefreshEnabled(): Boolean {
        return preferences.getBoolean(KEY_PREF_AUTO_REFRESH_ENABLED, false)
    }

    private fun getAutoRefreshPeriod(): Long {
        val hours = preferences.getString(KEY_PREF_AUTO_REFRESH_PERIOD, "0") ?: "0"
        // Convert hours to milliseconds
        return hours.toLong() * 60 * 60 * 1000
    }

    private fun getAutoRefreshWifiOnly(): Boolean {
        return preferences.getBoolean(KEY_PREF_AUTO_REFRESH_WIFI_ONLY, false)
    }

    private fun getPrevAutoRefreshTime(): Long {
        return preferences.getLong(KEY_LAST_AUTO_REFRESH_TIME, 0)
    }

    private fun setPrevAutoRefreshTime(time: Long) {
        preferences.edit().putLong(KEY_LAST_AUTO_REFRESH_TIME, time).apply()
    }

    private fun checkNetwork(): Boolean {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = connManager.activeNetworkInfo

        val connected = net != null && net.isConnected
        val metered = ConnectivityManagerCompat.isActiveNetworkMetered(connManager)
        val unmeteredOnly = getAutoRefreshWifiOnly()

        val okay = connected && !(metered && unmeteredOnly)

        Log.i(
            TAG,
            "connected=$connected, metered=$metered, unmeteredOnly=$unmeteredOnly, " +
                    "checkNetwork() ${if (okay) "passes" else "fails"}."
        )

        return okay
    }

    private fun checkBackup(): Boolean {
        return preferences.getBoolean(KEY_PREF_CONFIRMED_BACKUP, false)
    }

    fun rescheduleAlarm() {
        NetworkStateReceiver.disable(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, Service::class.java)
        val intentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getService(context, 0, intent, intentFlag)

        if (getAutoRefreshEnabled() && getAutoRefreshPeriod() != 0L) {
            val alarmTime = getPrevAutoRefreshTime() + getAutoRefreshPeriod()
            Log.i(TAG, "Scheduling auto refresh alarm for $alarmTime.")
            alarmManager.set(AlarmManager.RTC, alarmTime, pendingIntent)
        } else {
            Log.i(TAG, "Cancelling auto refresh alarm.")
            alarmManager.cancel(pendingIntent)
        }
    }

    class Service : IntentService(Service::class.java.name) {
        companion object {
            private val TAG = Service::class.java.name
        }

        override fun onHandleIntent(intent: Intent?) {
            val helper = getInstance(applicationContext)

            if (helper.checkNetwork() && helper.checkBackup()) {
                Log.i(TAG, "Refreshing all shows.")
                AsyncTask().executeAsync(RefreshAllShowsTask())
                helper.setPrevAutoRefreshTime(System.currentTimeMillis())
                helper.rescheduleAlarm()
            } else {
                NetworkStateReceiver.enable(this)
            }
        }
    }

    class BootReceiver : BroadcastReceiver() {
        companion object {
            private val TAG = BootReceiver::class.java.name
        }

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                Log.i(TAG, "Boot received.")
                // Ensure that the auto-refresh alarm is scheduled
                getInstance(context.applicationContext).rescheduleAlarm()
            }
        }
    }

    // This receiver is disabled by default in the manifest.
    // It should only be enabled when needed, and should be
    // disabled again straight afterwards.
    class NetworkStateReceiver : BroadcastReceiver() {
        companion object {
            private val TAG = NetworkStateReceiver::class.java.name

            fun enable(context: Context) {
                val packageManager = context.packageManager
                val receiver = ComponentName(context, NetworkStateReceiver::class.java)

                if (packageManager.getComponentEnabledSetting(receiver) !=
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                ) {
                    Log.i(TAG, "Enabling network state receiver.")
                }

                packageManager.setComponentEnabledSetting(
                    receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            fun disable(context: Context) {
                val packageManager = context.packageManager
                val receiver = ComponentName(context, NetworkStateReceiver::class.java)

                if (packageManager.getComponentEnabledSetting(receiver) !=
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                ) {
                    Log.i(TAG, "Disabling network state receiver.")
                }

                packageManager.setComponentEnabledSetting(
                    receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }

        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "Network state change received.")
            val helper = getInstance(context.applicationContext)
            if (helper.checkNetwork()) {
                helper.rescheduleAlarm()
            }
        }
    }
}
