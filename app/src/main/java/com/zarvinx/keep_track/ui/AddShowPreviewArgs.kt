package com.zarvinx.keep_track.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddShowPreviewArgs(
    val tmdbId: Int,
    val name: String,
    val language: String,
    val overview: String,
    val firstAiredMillis: Long?
) : Parcelable
