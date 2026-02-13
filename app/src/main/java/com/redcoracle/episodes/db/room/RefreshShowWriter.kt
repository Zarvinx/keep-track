package com.redcoracle.episodes.db.room

import android.content.Context
import android.content.ContentResolver
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.tvdb.Episode
import com.redcoracle.episodes.tvdb.Show
import org.apache.commons.collections4.map.MultiKeyMap

/**
 * Transitional writer for refresh-show operations.
 *
 * It attempts a Room transaction first, then falls back to the legacy provider-based
 * implementation when Room cannot open due schema validation mismatch on existing installs.
 */
class RefreshShowWriter(
    context: Context,
    private val contentResolver: ContentResolver
) {
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val refreshDao: RefreshShowRoomDao = roomDb.refreshShowDao()

    fun refreshShow(
        showId: Int,
        show: Show,
        episodes: MutableList<Episode>,
        fallback: () -> Unit
    ) {
        val usedRoom = runCatching {
            refreshWithRoom(showId, show, episodes)
            true
        }.getOrElse {
            fallback()
            false
        }

        if (usedRoom) {
            contentResolver.notifyChange(ShowsProvider.CONTENT_URI_SHOWS, null)
            contentResolver.notifyChange(ShowsProvider.CONTENT_URI_EPISODES, null)
        }
    }

    private fun refreshWithRoom(showId: Int, show: Show, episodes: MutableList<Episode>) {
        roomDb.runInTransaction {
            refreshDao.updateShow(
                showId = showId,
                tvdbId = show.tvdbId.takeIf { it > 0 },
                tmdbId = show.tmdbId,
                imdbId = show.imdbId,
                name = show.name,
                language = show.language,
                overview = show.overview,
                firstAired = show.firstAired?.time?.div(1000),
                bannerPath = show.bannerPath,
                fanartPath = show.fanartPath,
                posterPath = show.posterPath,
                status = show.status
            )

            val seasonPairMap = MultiKeyMap<Any, Episode>()
            val seen = HashSet<String>()
            val tmdbMap = HashMap<Int, Episode>()

            episodes.forEach { episode ->
                if (episode.tmdbId > 0) {
                    tmdbMap[episode.tmdbId] = episode
                }
                seasonPairMap.put(episode.seasonNumber, episode.episodeNumber, episode)
            }

            val existingRows = refreshDao.getEpisodesForShow(showId)
            for (row in existingRows) {
                var matched = row.tmdbId?.let { tmdbMap[it] }
                if (matched == null) {
                    matched = seasonPairMap.get(
                        row.seasonNumber ?: Int.MIN_VALUE,
                        row.episodeNumber ?: Int.MIN_VALUE
                    )
                    if (matched != null) {
                        if (seen.contains(matched.identifier())) {
                            refreshDao.deleteEpisodeById(row.id)
                        } else {
                            seen.add(matched.identifier())
                            episodes.remove(matched)
                        }
                        continue
                    }
                } else if (seen.contains(matched.identifier())) {
                    refreshDao.deleteEpisodeById(row.id)
                    continue
                } else {
                    seen.add(matched.identifier())
                }

                if (matched == null) {
                    refreshDao.deleteEpisodeById(row.id)
                } else {
                    refreshDao.updateEpisode(
                        episodeId = row.id,
                        showId = showId,
                        tvdbId = matched.tvdbId.takeIf { it > 0 },
                        tmdbId = matched.tmdbId.takeIf { it > 0 },
                        imdbId = matched.imdbId,
                        name = matched.name,
                        language = matched.language,
                        overview = matched.overview,
                        episodeNumber = matched.episodeNumber,
                        seasonNumber = matched.seasonNumber,
                        firstAired = matched.firstAired?.time?.div(1000)
                    )
                    episodes.remove(matched)
                }
            }

            episodes.forEach { episode ->
                refreshDao.insertEpisode(
                    showId = showId,
                    tvdbId = episode.tvdbId.takeIf { it > 0 },
                    tmdbId = episode.tmdbId.takeIf { it > 0 },
                    imdbId = episode.imdbId,
                    name = episode.name,
                    language = episode.language,
                    overview = episode.overview,
                    episodeNumber = episode.episodeNumber,
                    seasonNumber = episode.seasonNumber,
                    firstAired = episode.firstAired?.time?.div(1000)
                )
            }
        }
    }
}
