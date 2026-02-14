package com.redcoracle.episodes.services

import android.content.ContentResolver
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.R
import com.redcoracle.episodes.RefreshShowUtil.refreshShow
import com.redcoracle.episodes.db.room.AppDatabase
import java.util.concurrent.Callable

class RefreshAllShowsTask : Callable<Void?> {
    override fun call(): Void? {
        val context: Context = EpisodesApplication.instance.applicationContext
        val resolver: ContentResolver = context.contentResolver
        val shows = AppDatabase.getInstance(context).appReadDao().getAllShowsForRefresh()
        val total = shows.size

        val notificationManager = NotificationManagerCompat.from(context)
        val notificationBuilder = NotificationCompat.Builder(context, "episodes_channel_id")
        notificationBuilder
            .setContentTitle("Refreshing Shows")
            .setSmallIcon(R.drawable.ic_show_starred)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        var current = 0
        notificationBuilder.setProgress(total, current, false)
        notificationManager.notify(0, notificationBuilder.build())

        for (show in shows) {
            val showId = show.id
            val showName = show.name
            notificationBuilder.setContentText(showName)
            notificationBuilder.setProgress(total, current, false)
            notificationManager.notify(0, notificationBuilder.build())
            refreshShow(showId, resolver)
            current += 1
        }

        notificationBuilder.setContentText("Refresh complete!").setProgress(0, 0, false)
        notificationManager.notify(0, notificationBuilder.build())

        return null
    }
}
