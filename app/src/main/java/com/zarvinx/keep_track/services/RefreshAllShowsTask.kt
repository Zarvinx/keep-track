package com.zarvinx.keep_track.services

import android.content.ContentResolver
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zarvinx.keep_track.KeepTrackApplication
import com.zarvinx.keep_track.R
import com.zarvinx.keep_track.RefreshShowUtil.refreshShow
import com.zarvinx.keep_track.db.room.AppDatabase
import java.util.concurrent.Callable

/**
 * Background task that refreshes every show in the library.
 *
 * Progress and final summary are reported through notifications.
 */
class RefreshAllShowsTask : Callable<Void?> {
    /**
     * Runs a full-library refresh and posts a success ratio summary (X/Y).
     */
    override fun call(): Void? {
        val context: Context = KeepTrackApplication.instance.applicationContext
        val resolver: ContentResolver = context.contentResolver
        val shows = AppDatabase.getInstance(context).appReadDao().getAllShowsForRefresh()
        val total = shows.size

        val notificationManager = NotificationManagerCompat.from(context)
        val notificationBuilder = NotificationCompat.Builder(context, "keep_track_channel_id")
        notificationBuilder
            .setContentTitle(context.getString(R.string.refreshing_shows_title))
            .setSmallIcon(R.drawable.ic_show_starred)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (total == 0) {
            notificationBuilder
                .setContentText(context.getString(R.string.refresh_complete_none))
                .setProgress(0, 0, false)
            notificationManager.notify(0, notificationBuilder.build())
            return null
        }

        var current = 0
        var successCount = 0
        notificationBuilder.setProgress(total, current, false)
        notificationManager.notify(0, notificationBuilder.build())

        for (show in shows) {
            val showId = show.id
            val showName = show.name
            notificationBuilder.setContentText(showName)
            notificationBuilder.setProgress(total, current, false)
            notificationManager.notify(0, notificationBuilder.build())
            val refreshed = refreshShow(showId, resolver, logFailures = false)
            if (refreshed) {
                successCount += 1
            }
            current += 1
        }

        val failureCount = total - successCount
        if (failureCount > 0) {
            Log.w(
                RefreshAllShowsTask::class.java.name,
                "Refresh completed with failures: $successCount/$total succeeded."
            )
        }
        notificationBuilder
            .setContentText(context.getString(R.string.refresh_complete_summary, successCount, total))
            .setSubText(
                if (failureCount > 0) {
                    context.getString(R.string.refresh_partial_failures, failureCount)
                } else {
                    null
                }
            )
            .setProgress(0, 0, false)
        notificationManager.notify(0, notificationBuilder.build())

        return null
    }
}
