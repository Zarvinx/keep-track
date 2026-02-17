package com.zarvinx.keep_track.db.room

import com.zarvinx.keep_track.tvdb.Episode
import com.zarvinx.keep_track.tvdb.Show

internal fun Show.normalizedTvdbId(): Int? = tvdbId.takeIf { it > 0 }

internal fun Show.normalizedImdbId(): String? = imdbId?.takeIf { it.isNotBlank() }

internal fun Show.toShowInsertEntityOrNull(): ShowEntity? {
    val showName = name ?: return null
    return ShowEntity(
        id = null,
        tvdbId = normalizedTvdbId(),
        tmdbId = tmdbId,
        imdbId = normalizedImdbId(),
        name = showName,
        language = language,
        overview = overview,
        firstAired = firstAired?.time?.div(1000),
        starred = 0,
        archived = 0,
        bannerPath = bannerPath,
        fanartPath = fanartPath,
        posterPath = posterPath,
        notes = null,
        status = status
    )
}

internal fun Show.toRefreshShowUpdate(showId: Int): RefreshShowUpdate {
    return RefreshShowUpdate(
        id = showId,
        tvdbId = normalizedTvdbId(),
        tmdbId = tmdbId,
        imdbId = normalizedImdbId(),
        name = name ?: "",
        language = language,
        overview = overview,
        firstAired = firstAired?.time?.div(1000),
        bannerPath = bannerPath,
        fanartPath = fanartPath,
        posterPath = posterPath,
        status = status
    )
}

private fun Episode.normalizedTvdbId(): Int? = tvdbId?.takeIf { it > 0 }

private fun Episode.normalizedTmdbId(): Int? = tmdbId?.takeIf { it > 0 }

private fun Episode.normalizedImdbId(): String? = imdbId?.takeIf { it.isNotBlank() }

internal fun Episode.toEpisodeInsertEntity(showId: Int): EpisodeEntity {
    return EpisodeEntity(
        id = 0,
        tvdbId = normalizedTvdbId(),
        tmdbId = normalizedTmdbId(),
        imdbId = normalizedImdbId(),
        showId = showId,
        name = name ?: "",
        language = language,
        overview = overview,
        episodeNumber = episodeNumber,
        seasonNumber = seasonNumber,
        firstAired = firstAired?.time?.div(1000),
        watched = null
    )
}

internal fun Episode.toRefreshEpisodeUpdate(episodeId: Int, showId: Int): RefreshEpisodeUpdate {
    return RefreshEpisodeUpdate(
        id = episodeId,
        showId = showId,
        tvdbId = normalizedTvdbId(),
        tmdbId = normalizedTmdbId(),
        imdbId = normalizedImdbId(),
        name = name ?: "",
        language = language,
        overview = overview,
        episodeNumber = episodeNumber,
        seasonNumber = seasonNumber,
        firstAired = firstAired?.time?.div(1000)
    )
}
