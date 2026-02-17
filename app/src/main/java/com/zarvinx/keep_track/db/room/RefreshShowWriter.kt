package com.zarvinx.keep_track.db.room

import android.content.Context
import com.zarvinx.keep_track.tvdb.Episode
import com.zarvinx.keep_track.tvdb.Show
import org.apache.commons.collections4.map.MultiKeyMap

/**
 * Applies TMDB refresh results to local show and episode rows.
 *
 * Writes are performed in a single Room transaction.
 */
class RefreshShowWriter(
    context: Context
) {
    private val roomDb: AppDatabase = AppDatabase.getInstance(context.applicationContext)
    private val refreshDao: RefreshShowRoomDao = roomDb.refreshShowDao()

    /**
     * Reconciles one show and its episode set against remote data.
     */
    fun refreshShow(
        showId: Int,
        show: Show,
        episodes: MutableList<Episode>
    ) {
        refreshWithRoom(showId, show, episodes)
    }

    private fun refreshWithRoom(showId: Int, show: Show, episodes: MutableList<Episode>) {
        roomDb.runInTransaction {
            refreshDao.updateShow(show.toRefreshShowUpdate(showId))

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
                    refreshDao.updateEpisode(matched.toRefreshEpisodeUpdate(row.id, showId))
                    episodes.remove(matched)
                }
            }

            episodes.forEach { episode ->
                refreshDao.insertEpisode(episode.toEpisodeInsertEntity(showId))
            }
        }
    }
}
