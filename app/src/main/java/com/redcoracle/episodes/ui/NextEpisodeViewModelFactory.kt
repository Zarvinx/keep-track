package com.redcoracle.episodes.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class NextEpisodeViewModelFactory(
    private val application: Application,
    private val showId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NextEpisodeViewModel::class.java)) {
            return NextEpisodeViewModel(application, showId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
