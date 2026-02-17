package com.zarvinx.keep_track.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room mapping of the legacy `shows` table.
 *
 * Field affinities intentionally mirror existing on-device schema expectations.
 */
@Entity(
    tableName = "shows",
    indices = [
        Index(value = ["name"])
    ]
)
data class ShowEntity(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    val id: Int?,
    @ColumnInfo(name = "tvdb_id")
    val tvdbId: Int?,
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int?,
    @ColumnInfo(name = "imdb_id")
    val imdbId: String?,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "language")
    val language: String?,
    @ColumnInfo(name = "overview")
    val overview: String?,
    @ColumnInfo(name = "first_aired")
    val firstAired: Long?,
    @ColumnInfo(name = "starred")
    val starred: Int?,
    @ColumnInfo(name = "archived")
    val archived: Int?,
    @ColumnInfo(name = "banner_path")
    val bannerPath: String?,
    @ColumnInfo(name = "fanart_path")
    val fanartPath: String?,
    @ColumnInfo(name = "poster_path")
    val posterPath: String?,
    @ColumnInfo(name = "notes")
    val notes: String?,
    @ColumnInfo(name = "status")
    val status: String?
)
