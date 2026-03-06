package com.zarvinx.keep_track.ui

import android.os.Parcel
import android.os.Parcelable

data class AddShowPreviewArgs(
    val tmdbId: Int,
    val name: String,
    val language: String,
    val overview: String,
    val firstAiredMillis: Long?
) : Parcelable {
    private constructor(parcel: Parcel) : this(
        tmdbId = parcel.readInt(),
        name = parcel.readString().orEmpty(),
        language = parcel.readString().orEmpty(),
        overview = parcel.readString().orEmpty(),
        firstAiredMillis = parcel.readValue(Long::class.java.classLoader) as Long?
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(tmdbId)
        parcel.writeString(name)
        parcel.writeString(language)
        parcel.writeString(overview)
        parcel.writeValue(firstAiredMillis)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AddShowPreviewArgs> {
        override fun createFromParcel(parcel: Parcel): AddShowPreviewArgs = AddShowPreviewArgs(parcel)

        override fun newArray(size: Int): Array<AddShowPreviewArgs?> = arrayOfNulls(size)
    }
}
