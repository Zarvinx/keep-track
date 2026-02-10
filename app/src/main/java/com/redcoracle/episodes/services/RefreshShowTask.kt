package com.redcoracle.episodes.services

import android.content.Context
import com.redcoracle.episodes.EpisodesApplication
import com.redcoracle.episodes.RefreshShowUtil
import java.util.concurrent.Callable

class RefreshShowTask(private val showId: Int) : Callable<Void?> {
    private val context: Context = EpisodesApplication.instance.applicationContext

    override fun call(): Void? {
        RefreshShowUtil.refreshShow(showId, context.contentResolver)
        return null
    }

    companion object {
        private val TAG = RefreshShowTask::class.java.name
    }
}
