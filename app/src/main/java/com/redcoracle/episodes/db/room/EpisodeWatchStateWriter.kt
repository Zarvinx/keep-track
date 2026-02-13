package com.redcoracle.episodes.db.room

import android.content.ContentValues
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider

/**
 * Transitional writer for watch-state updates during incremental Room migration.
 *
 * Why fallback exists:
 * Room validates schema declarations strictly. Existing app installs may have legacy
 * `episodes` declarations from SQLiteOpenHelper (e.g. VARCHAR/DATE/BOOLEAN) that do not
 * match Room's inferred TEXT/INTEGER/INTEGER expectations.
 *
 * Safety behavior:
 * - Attempt Room DAO write first.
 * - Fallback to legacy ContentResolver write on failure.
 * - Always notify ContentProvider URIs so existing observers still refresh.
 *
 * Cleanup target:
 * Remove fallback paths once the app has a fully aligned Room schema/migration strategy.
 * See docs/room-watchstate-migration.md for the full plan.
 */
class EpisodeWatchStateWriter(context: Context) {
    private val contentResolver: ContentResolver = context.applicationContext.contentResolver
    private val episodesDao: EpisodesRoomDao =
        EpisodesRoomDatabase.getInstance(context.applicationContext).episodesDao()

    suspend fun setEpisodeWatched(episodeId: Int, watched: Boolean) {
        // TODO(room-migration): Remove fallback path after Room schema alignment is complete.
        runCatching {
            episodesDao.updateEpisodeWatched(episodeId, watched.toDbInt())
        }.getOrElse {
            val uri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES, episodeId.toString())
            val values = ContentValues().apply {
                put(EpisodesTable.COLUMN_WATCHED, watched.toDbInt())
            }
            contentResolver.update(uri, values, null, null)
        }
        notifyEpisodesChanged(episodeId)
    }

    suspend fun setSeasonWatched(showId: Int, seasonNumber: Int, watched: Boolean) {
        val nowSeconds = System.currentTimeMillis() / 1000
        // TODO(room-migration): Remove fallback path after Room schema alignment is complete.
        runCatching {
            if (watched) {
                episodesDao.markAiredSeasonWatched(showId, seasonNumber, nowSeconds)
            } else {
                episodesDao.updateSeasonWatched(showId, seasonNumber, 0)
            }
        }.getOrElse {
            val values = ContentValues().apply {
                put(EpisodesTable.COLUMN_WATCHED, watched.toDbInt())
            }
            var selection = "${EpisodesTable.COLUMN_SHOW_ID}=? AND ${EpisodesTable.COLUMN_SEASON_NUMBER}=?"
            val selectionArgs = mutableListOf(showId.toString(), seasonNumber.toString())
            if (watched) {
                selection += " AND ${EpisodesTable.COLUMN_FIRST_AIRED} <= ? AND ${EpisodesTable.COLUMN_FIRST_AIRED} IS NOT NULL"
                selectionArgs.add(nowSeconds.toString())
            }
            contentResolver.update(
                ShowsProvider.CONTENT_URI_EPISODES,
                values,
                selection,
                selectionArgs.toTypedArray()
            )
        }
        notifyEpisodesChanged()
    }

    suspend fun setShowWatched(showId: Int, watched: Boolean) {
        // TODO(room-migration): Remove fallback path after Room schema alignment is complete.
        runCatching {
            episodesDao.updateShowWatched(showId, watched.toDbInt())
        }.getOrElse {
            val values = ContentValues().apply {
                put(EpisodesTable.COLUMN_WATCHED, watched.toDbInt())
            }
            contentResolver.update(
                ShowsProvider.CONTENT_URI_EPISODES,
                values,
                "${EpisodesTable.COLUMN_SHOW_ID}=? AND ${EpisodesTable.COLUMN_SEASON_NUMBER}!=?",
                arrayOf(showId.toString(), "0")
            )
        }
        notifyEpisodesChanged()
    }

    private fun notifyEpisodesChanged(episodeId: Int? = null) {
        contentResolver.notifyChange(ShowsProvider.CONTENT_URI_EPISODES, null)
        if (episodeId != null) {
            val episodeUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES, episodeId.toString())
            contentResolver.notifyChange(episodeUri, null)
        }
    }

    private fun Boolean.toDbInt(): Int = if (this) 1 else 0
}
