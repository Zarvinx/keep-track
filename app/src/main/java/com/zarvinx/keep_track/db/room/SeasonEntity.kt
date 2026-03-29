package com.zarvinx.keep_track.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "seasons",
    primaryKeys = ["show_id", "season_number"]
)
data class SeasonEntity(
    @ColumnInfo(name = "show_id") val showId: Int,
    @ColumnInfo(name = "season_number") val seasonNumber: Int,
    @ColumnInfo(name = "name") val name: String?
)
