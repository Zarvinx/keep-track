package com.redcoracle.episodes.services

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.R
import com.redcoracle.episodes.RefreshShowUtil.refreshShow
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
import java.util.concurrent.Callable

class RefreshAllShowsTask : Callable<Void?> {
    override fun call(): Void? {
        val context: Context = EpisodesApplication.instance.applicationContext
        val resolver: ContentResolver = context.contentResolver
        val showUri: Uri = ShowsProvider.CONTENT_URI_SHOWS
        val projection = arrayOf(
            ShowsTable.COLUMN_ID,
            ShowsTable.COLUMN_NAME
        )
        val sort = "${ShowsTable.COLUMN_NAME} ASC"
        val cursor: Cursor = resolver.query(showUri, projection, null, null, sort)
            ?: return null
        
        val idColumnIndex = cursor.getColumnIndex(ShowsTable.COLUMN_ID)
        val nameColumnIndex = cursor.getColumnIndex(ShowsTable.COLUMN_NAME)
        val total = cursor.count

        val notificationManager = NotificationManagerCompat.from(context)
        val notificationBuilder = NotificationCompat.Builder(context, "episodes_channel_id")
        notificationBuilder
            .setContentTitle("Refreshing Shows")
            .setSmallIcon(R.drawable.ic_show_starred)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        var current = 0
        notificationBuilder.setProgress(total, current, false)
        notificationManager.notify(0, notificationBuilder.build())

        cursor.moveToFirst()
        do {
            val showId = cursor.getInt(idColumnIndex)
            val showName = cursor.getString(nameColumnIndex)
            notificationBuilder.setContentText(showName)
            notificationBuilder.setProgress(total, current, false)
            notificationManager.notify(0, notificationBuilder.build())
            refreshShow(showId, resolver)
            current += 1
        } while (cursor.moveToNext())
        
        cursor.close()
        
        notificationBuilder.setContentText("Refresh complete!").setProgress(0, 0, false)
        notificationManager.notify(0, notificationBuilder.build())
        
        return null
    }
}
