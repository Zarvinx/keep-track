package com.zarvinx.keep_track.services

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.zarvinx.keep_track.JsonBackup
import com.zarvinx.keep_track.KeepTrackApplication
import com.zarvinx.keep_track.R
import java.io.IOException
import java.util.concurrent.Callable

class RestoreTask(private val backupUriString: String) : Callable<Void?> {
    private val context: Context = KeepTrackApplication.instance.applicationContext

    override fun call(): Void? {
        val backupUri = Uri.parse(backupUriString)

        try {
            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                val json = inputStream.bufferedReader(Charsets.UTF_8).readText()
                JsonBackup.importFromJson(context, json)
            }

            ContextCompat.getMainExecutor(context).execute {
                Toast.makeText(
                    context,
                    context.getString(R.string.restore_success_message),
                    Toast.LENGTH_LONG
                ).show()
            }

            Log.i(TAG, "Library restored successfully.")
        } catch (e: IOException) {
            Log.e(TAG, "Error restoring library: $e")
        }

        return null
    }

    companion object {
        private val TAG = RestoreTask::class.java.name
    }
}
