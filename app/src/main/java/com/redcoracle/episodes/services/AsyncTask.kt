package com.redcoracle.episodes.services

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper class for executing background tasks using Kotlin coroutines.
 * Replaces the deprecated android.os.AsyncTask.
 */
class AsyncTask {
    /**
     * Execute a callable task on the IO dispatcher.
     * @param callable The task to execute
     */
    fun <R> executeAsync(
        callable: java.util.concurrent.Callable<R>,
        onSuccess: ((R) -> Unit)? = null,
        onError: ((Throwable) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = callable.call()
                if (onSuccess != null) {
                    withContext(Dispatchers.Main) {
                        onSuccess(result)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background task failed: ${callable.javaClass.simpleName}", e)
                if (onError != null) {
                    withContext(Dispatchers.Main) {
                        onError(e)
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = AsyncTask::class.java.name
    }
}
