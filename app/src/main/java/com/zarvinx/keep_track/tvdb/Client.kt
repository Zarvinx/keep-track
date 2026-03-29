/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
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

package com.zarvinx.keep_track.tvdb

import android.util.Log
import com.zarvinx.keep_track.KeepTrackApplication
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import retrofit2.Response
import java.io.IOException

class Client {
    private val TAG = Client::class.java.name
    private val tmdb = KeepTrackApplication.instance.tmdbClient!!

    private fun throttleRequestRate() {
        synchronized(REQUEST_LOCK) {
            val now = System.currentTimeMillis()
            val waitMs = (lastRequestAtMs + MIN_REQUEST_INTERVAL_MS) - now
            if (waitMs > 0) sleep(waitMs)
            lastRequestAtMs = System.currentTimeMillis()
        }
    }

    private fun sleep(durationMs: Long) {
        try {
            Thread.sleep(durationMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun parseRetryAfterMillis(response: Response<*>): Long {
        val retryAfter = response.headers()["Retry-After"] ?: return -1L
        return try {
            val seconds = retryAfter.trim().toLong()
            if (seconds >= 0) seconds * 1000L else -1L
        } catch (e: NumberFormatException) {
            -1L
        }
    }

    private fun <T> bodyOrNull(response: Response<T>?): T? {
        if (response == null) return null
        if (response.isSuccessful) return response.body()
        response.errorBody()?.close()
        Log.w(TAG, "TMDB request failed: ${response.code()} ${response.message()}")
        return null
    }

    private fun <T> executeWithRetry(request: () -> Response<T>): Response<T> {
        var ioFailures = 0
        for (attempt in 0..MAX_429_RETRIES) {
            throttleRequestRate()

            val response: Response<T>
            try {
                response = request()
            } catch (e: IOException) {
                if (ioFailures >= MAX_IO_RETRIES) throw e
                ioFailures++
                val backoffMs = BASE_RETRY_DELAY_MS * ioFailures
                Log.w(TAG, "TMDB I/O failure, retrying in ${backoffMs}ms", e)
                sleep(backoffMs)
                continue
            } catch (e: Exception) {
                throw IOException("TMDB request failed unexpectedly", e)
            }

            if (response.code() != 429) return response

            var retryAfterMs = parseRetryAfterMillis(response)
            if (retryAfterMs < 0L) retryAfterMs = BASE_RETRY_DELAY_MS * (attempt + 1L)

            response.errorBody()?.close()

            if (attempt == MAX_429_RETRIES) {
                Log.w(TAG, "TMDB rate limited after max retries.")
                return response
            }

            Log.w(TAG, "TMDB rate limited (429), retrying in ${retryAfterMs}ms")
            sleep(retryAfterMs)
        }
        throw IOException("Unreachable TMDB retry state")
    }

    fun searchShows(query: String, language: String?): List<Show> {
        return try {
            val response = executeWithRetry {
                tmdb.searchService().tv(query, null, language, null, false).execute()
            }
            val results = bodyOrNull(response)
            if (results != null) SearchShowsParser().parse(results, language ?: "") else emptyList()
        } catch (e: IOException) {
            Log.w(TAG, e)
            emptyList()
        }
    }

    fun getShow(showIds: HashMap<String, String>, language: String): Show? {
        return try {
            val includes = AppendToResponse(AppendToResponseItem.EXTERNAL_IDS)
            var lookupResult: TvShow? = null

            if (showIds["tmdbId"] != null) {
                val tmdbId = showIds["tmdbId"]!!.toInt()
                lookupResult = bodyOrNull(executeWithRetry {
                    tmdb.tvService().tv(tmdbId, language, includes).execute()
                })
            }

            if (lookupResult == null && showIds["tvdbId"] != null) {
                val findResults = bodyOrNull(executeWithRetry {
                    tmdb.findService().find(showIds["tvdbId"], ExternalSource.TVDB_ID, language).execute()
                })
                if (findResults?.tv_results?.isNotEmpty() == true) {
                    val sparseShow = findResults.tv_results[0]
                    lookupResult = bodyOrNull(executeWithRetry {
                        tmdb.tvService().tv(sparseShow.id, language, includes).execute()
                    })
                }
            }

            if (lookupResult == null && showIds["imdbId"] != null) {
                val findResults = bodyOrNull(executeWithRetry {
                    tmdb.findService().find(showIds["imdbId"], ExternalSource.IMDB_ID, language).execute()
                })
                if (findResults?.tv_results?.isNotEmpty() == true) {
                    val sparseShow = findResults.tv_results[0]
                    lookupResult = bodyOrNull(executeWithRetry {
                        tmdb.tvService().tv(sparseShow.id, language, includes).execute()
                    })
                }
            }

            lookupResult?.let { series ->
                GetShowParser().parse(series, language)?.also { show ->
                    show.episodes = getEpisodesForShow(series, language)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, e)
            null
        }
    }

    fun getShow(id: Int, language: String, includeEpisodes: Boolean): Show? {
        return try {
            val includes = AppendToResponse(AppendToResponseItem.EXTERNAL_IDS)
            val seriesResponse = executeWithRetry {
                tmdb.tvService().tv(id, language, includes).execute()
            }
            Log.d(TAG, "Received response ${seriesResponse.code()}: ${seriesResponse.message()}")
            val series = bodyOrNull(seriesResponse) ?: return null
            val show = GetShowParser().parse(series, language) ?: return null
            if (includeEpisodes) {
                show.episodes = getEpisodesForShow(series, language)
            }
            show
        } catch (e: IOException) {
            Log.w(TAG, e)
            null
        }
    }

    fun getEpisodesForShow(series: TvShow, language: String): List<Episode> {
        val episodeCount = series.number_of_episodes ?: 64
        val episodes = ArrayList<Episode>(episodeCount)
        val episodesParser = GetEpisodesParser()
        if (series.number_of_seasons != null) {
            for (seasonStub in series.seasons.orEmpty()) {
                try {
                    val includes = AppendToResponse(AppendToResponseItem.EXTERNAL_IDS)
                    val seasonNumber = seasonStub.season_number ?: continue
                    val seasonResponse = executeWithRetry {
                        tmdb.tvSeasonsService().season(series.id, seasonNumber, language, includes).execute()
                    }
                    val season: TvSeason = bodyOrNull(seasonResponse) ?: continue
                    episodesParser.parse(season.episodes)?.let { episodes.addAll(it) }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return episodes
    }

    companion object {
        private val REQUEST_LOCK = Any()
        private const val MIN_REQUEST_INTERVAL_MS = 35L
        private const val MAX_429_RETRIES = 4
        private const val MAX_IO_RETRIES = 2
        private const val BASE_RETRY_DELAY_MS = 1000L
        private var lastRequestAtMs = 0L
    }
}
