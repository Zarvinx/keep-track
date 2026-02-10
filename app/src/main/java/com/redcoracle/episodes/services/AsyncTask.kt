package com.redcoracle.episodes.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper class for executing background tasks using Kotlin coroutines.
 * Replaces the deprecated android.os.AsyncTask.
 */
class AsyncTask {
    /**
     * Execute a callable task on the IO dispatcher.
     * @param callable The task to execute
     */
    fun <R> executeAsync(callable: java.util.concurrent.Callable<R>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                callable.call()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
