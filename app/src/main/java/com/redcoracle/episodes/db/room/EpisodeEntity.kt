package com.redcoracle.episodes.db.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodes")
/**
 * Room view of legacy `episodes` table.
 *
 * Note: Existing installs may have legacy SQL declarations that differ by declared type
 * names (e.g. VARCHAR/DATE/BOOLEAN). SQLite can store compatible values, but Room schema
 * validation can still reject mismatched declarations until a full migration normalizes them.
 */
data class EpisodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "tvdb_id")
    val tvdbId: Int?,
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int?,
    @ColumnInfo(name = "imdb_id")
    val imdbId: String?,
    @ColumnInfo(name = "show_id")
    val showId: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "language")
    val language: String?,
    @ColumnInfo(name = "overview")
    val overview: String?,
    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int?,
    @ColumnInfo(name = "season_number")
    val seasonNumber: Int?,
    @ColumnInfo(name = "first_aired")
    val firstAired: Long?,
    @ColumnInfo(name = "watched")
    val watched: Int?
)
