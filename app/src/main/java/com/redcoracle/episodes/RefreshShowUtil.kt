/*
 * Copyright (C) 2014 Jamie Nicol <jamie@thenicols.net>
 * Copyright (C) 2026 Zarvinx (Kotlin conversion)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redcoracle.episodes

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.util.SparseArray
import com.redcoracle.episodes.db.EpisodesTable
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.db.ShowsTable
import com.redcoracle.episodes.db.room.RefreshShowWriter
import com.redcoracle.episodes.tvdb.Client
import com.redcoracle.episodes.tvdb.Episode
import org.apache.commons.collections4.map.MultiKeyMap

object RefreshShowUtil {
    private val TAG = RefreshShowUtil::class.java.name

    @JvmStatic
    fun refreshShow(showId: Int, contentResolver: ContentResolver) {
        Log.i(TAG, "Refreshing show $showId")

        val tmdbClient = Client()
        val preferences = Preferences.getSharedPreferences()

        val showLanguage = preferences?.getString("pref_language", "en") ?: "en"
        val showIds = getShowIds(showId, contentResolver)
        val show = tmdbClient.getShow(showIds, showLanguage)

        if (show != null) {
            show.episodes?.let { episodes ->
                val roomEpisodes = episodes.toMutableList()
                val legacyEpisodes = episodes.toMutableList()
                val writer = RefreshShowWriter(
                    context = EpisodesApplication.instance.applicationContext,
                    contentResolver = contentResolver
                )
                writer.refreshShow(
                    showId = showId,
                    show = show,
                    episodes = roomEpisodes
                ) {
                    updateShow(showId, show, contentResolver)
                    updateEpisodes(showId, legacyEpisodes, contentResolver)
                }
            }
        }
    }

    private fun getShowIds(showId: Int, contentResolver: ContentResolver): HashMap<String, String> {
        val showUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, showId.toString())
        val projection = arrayOf(
            ShowsTable.COLUMN_TVDB_ID,
            ShowsTable.COLUMN_TMDB_ID,
            ShowsTable.COLUMN_IMDB_ID
        )
        
        val showIds = HashMap<String, String>()
        contentResolver.query(showUri, projection, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            val tvdbIdColumnIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_TVDB_ID)
            val tmdbIdColumnIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_TMDB_ID)
            val imdbIdColumnIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_IMDB_ID)
            
            showIds["tvdbId"] = cursor.getString(tvdbIdColumnIndex)
            showIds["tmdbId"] = cursor.getString(tmdbIdColumnIndex)
            showIds["imdbId"] = cursor.getString(imdbIdColumnIndex)
        }
        
        return showIds
    }

    private fun updateShow(showId: Int, show: com.redcoracle.episodes.tvdb.Show, contentResolver: ContentResolver) {
        val showValues = ContentValues().apply {
            if (show.tvdbId != 0) {
                put(ShowsTable.COLUMN_TVDB_ID, show.tvdbId)
            }
            put(ShowsTable.COLUMN_TMDB_ID, show.tmdbId)
            put(ShowsTable.COLUMN_IMDB_ID, show.imdbId)
            put(ShowsTable.COLUMN_NAME, show.name)
            put(ShowsTable.COLUMN_LANGUAGE, show.language)
            put(ShowsTable.COLUMN_OVERVIEW, show.overview)
            show.firstAired?.let { 
                put(ShowsTable.COLUMN_FIRST_AIRED, it.time / 1000)
            }
            put(ShowsTable.COLUMN_BANNER_PATH, show.bannerPath)
            put(ShowsTable.COLUMN_FANART_PATH, show.fanartPath)
            put(ShowsTable.COLUMN_POSTER_PATH, show.posterPath)
            put(ShowsTable.COLUMN_STATUS, show.status)
        }

        val showUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_SHOWS, showId.toString())
        contentResolver.update(showUri, showValues, null, null)
    }

    private fun updateEpisodes(showId: Int, episodes: MutableList<Episode>, contentResolver: ContentResolver) {
        // TODO: likely performance gains to be had in here
        val seasonPairMap = MultiKeyMap<Any, Episode>()
        val seen = HashSet<String>()
        val episodeMap = SparseArray<Episode>()
        val updates = ArrayList<ContentValues>()

        for (episode in episodes) {
            episodeMap.append(episode.tmdbId, episode)
            seasonPairMap.put(episode.seasonNumber, episode.episodeNumber, episode)
        }

        val cursor = getEpisodesCursor(showId, contentResolver)

        while (cursor.moveToNext()) {
            val idColumnIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID)
            val episodeId = cursor.getInt(idColumnIndex)
            val tmdbColumnIndex = cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_TMDB_ID)
            val episodeTmdbId = cursor.getInt(tmdbColumnIndex)

            var episode = episodeMap.get(episodeTmdbId)
            val episodeUri = Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES, episodeId.toString())

            if (episode == null) {
                // Unable to find episode by ID; try season/episode pair instead.
                // I think this should only happen when a show needs to migrate from TVDB->TMDB
                episode = seasonPairMap.get(
                    cursor.getInt(cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_SEASON_NUMBER)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_EPISODE_NUMBER))
                )
                
                if (episode != null) {
                    Log.d(TAG, "Matched by season/episode number: $episodeId")
                    if (seen.contains(episode.identifier())) {
                        // Already matched a different episode by season/episode pair,
                        // so this will fail on insert and should be deleted instead.
                        Log.d(TAG, "Deleting duplicate episode ${episode.identifier()} ($episodeId)")
                        contentResolver.delete(episodeUri, null, null)
                    } else {
                        seen.add(episode.identifier())
                        episodes.remove(episode)
                        continue
                    }
                }
            } else if (seen.contains(episode.identifier())) {
                Log.d(TAG, "Deleting previously seen episode ${episode.identifier()} (${episode.id})")
                contentResolver.delete(episodeUri, null, null)
                continue
            } else {
                Log.d(TAG, "Found match by TMDB ID: $episodeId")
                seen.add(episode.identifier())
            }

            if (episode == null) {
                Log.i(TAG, "No matches found. Deleting episode: $episodeId")
                contentResolver.delete(episodeUri, null, null)
            } else {
                val epValues = ContentValues().apply {
                    put(EpisodesTable.COLUMN_ID, episodeId)
                    put(EpisodesTable.COLUMN_SHOW_ID, showId)
                    put(EpisodesTable.COLUMN_TVDB_ID, episode.tvdbId)
                    put(EpisodesTable.COLUMN_TMDB_ID, episode.tmdbId)
                    put(EpisodesTable.COLUMN_IMDB_ID, episode.imdbId)
                    put(EpisodesTable.COLUMN_NAME, episode.name)
                    put(EpisodesTable.COLUMN_LANGUAGE, episode.language)
                    put(EpisodesTable.COLUMN_OVERVIEW, episode.overview)
                    put(EpisodesTable.COLUMN_EPISODE_NUMBER, episode.episodeNumber)
                    put(EpisodesTable.COLUMN_SEASON_NUMBER, episode.seasonNumber)
                    episode.firstAired?.let { 
                        put(EpisodesTable.COLUMN_FIRST_AIRED, it.time / 1000)
                    }
                }

                Log.i(TAG, "Updating episode $episodeId.")
                updates.add(epValues)

                // Remove episode from list of episodes returned by tvdb.
                // By the end of this function this list will only contain new episodes
                episodes.remove(episode)
            }
        }
        cursor.close()
        contentResolver.bulkInsert(ShowsProvider.CONTENT_URI_EPISODES, updates.toTypedArray())

        // Insert remaining new episodes
        for (episode in episodes) {
            val epValues = ContentValues().apply {
                put(EpisodesTable.COLUMN_SHOW_ID, showId)
                put(EpisodesTable.COLUMN_TVDB_ID, episode.tvdbId)
                put(EpisodesTable.COLUMN_TMDB_ID, episode.tmdbId)
                put(EpisodesTable.COLUMN_IMDB_ID, episode.imdbId)
                put(EpisodesTable.COLUMN_NAME, episode.name)
                put(EpisodesTable.COLUMN_LANGUAGE, episode.language)
                put(EpisodesTable.COLUMN_OVERVIEW, episode.overview)
                put(EpisodesTable.COLUMN_EPISODE_NUMBER, episode.episodeNumber)
                put(EpisodesTable.COLUMN_SEASON_NUMBER, episode.seasonNumber)
                episode.firstAired?.let { 
                    put(EpisodesTable.COLUMN_FIRST_AIRED, it.time / 1000)
                }
            }

            contentResolver.insert(ShowsProvider.CONTENT_URI_EPISODES, epValues)
        }
    }

    private fun getEpisodesCursor(showId: Int, contentResolver: ContentResolver): Cursor {
        val projection = arrayOf(
            EpisodesTable.COLUMN_ID,
            EpisodesTable.COLUMN_TVDB_ID,
            EpisodesTable.COLUMN_TMDB_ID,
            EpisodesTable.COLUMN_IMDB_ID,
            EpisodesTable.COLUMN_SEASON_NUMBER,
            EpisodesTable.COLUMN_EPISODE_NUMBER
        )
        val selection = "${EpisodesTable.COLUMN_SHOW_ID}=?"
        val selectionArgs = arrayOf(showId.toString())

        return contentResolver.query(
            ShowsProvider.CONTENT_URI_EPISODES,
            projection,
            selection,
            selectionArgs,
            null
        )!!
    }
}
