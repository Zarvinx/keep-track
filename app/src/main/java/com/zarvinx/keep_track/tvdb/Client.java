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

package com.zarvinx.keep_track.tvdb;

import android.util.Log;

import com.zarvinx.keep_track.KeepTrackApplication;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.entities.AppendToResponse;
import com.uwetrottmann.tmdb2.entities.BaseTvShow;
import com.uwetrottmann.tmdb2.entities.FindResults;
import com.uwetrottmann.tmdb2.entities.TvSeason;
import com.uwetrottmann.tmdb2.entities.TvShow;
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage;
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem;
import com.uwetrottmann.tmdb2.enumerations.ExternalSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import retrofit2.Response;

public class Client {
    private static final String TAG = Client.class.getName();
    private static final Object REQUEST_LOCK = new Object();
    // Keep headroom below TMDB's documented limit.
    private static final long MIN_REQUEST_INTERVAL_MS = 35L;
    private static final int MAX_429_RETRIES = 4;
    private static final int MAX_IO_RETRIES = 2;
    private static final long BASE_RETRY_DELAY_MS = 1000L;
    private static long lastRequestAtMs = 0L;
    private final Tmdb tmdb;

    public Client() {
        this.tmdb = KeepTrackApplication.getInstance().getTmdbClient();
    }

    private void throttleRequestRate() {
        synchronized (REQUEST_LOCK) {
            final long now = System.currentTimeMillis();
            final long waitMs = (lastRequestAtMs + MIN_REQUEST_INTERVAL_MS) - now;
            if (waitMs > 0) {
                sleep(waitMs);
            }
            lastRequestAtMs = System.currentTimeMillis();
        }
    }

    private void sleep(long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long parseRetryAfterMillis(Response<?> response) {
        String retryAfter = response.headers().get("Retry-After");
        if (retryAfter == null) {
            return -1L;
        }
        try {
            long seconds = Long.parseLong(retryAfter.trim());
            return seconds >= 0 ? seconds * 1000L : -1L;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private <T> T bodyOrNull(Response<T> response) {
        if (response == null) {
            return null;
        }
        if (response.isSuccessful()) {
            return response.body();
        }
        if (response.errorBody() != null) {
            response.errorBody().close();
        }
        Log.w(TAG, String.format("TMDB request failed: %d %s", response.code(), response.message()));
        return null;
    }

    private <T> Response<T> executeWithRetry(Callable<Response<T>> request) throws IOException {
        int ioFailures = 0;
        for (int attempt = 0; attempt <= MAX_429_RETRIES; attempt++) {
            throttleRequestRate();

            final Response<T> response;
            try {
                response = request.call();
            } catch (IOException e) {
                if (ioFailures >= MAX_IO_RETRIES) {
                    throw e;
                }
                ioFailures += 1;
                long backoffMs = BASE_RETRY_DELAY_MS * ioFailures;
                Log.w(TAG, String.format("TMDB I/O failure, retrying in %dms", backoffMs), e);
                sleep(backoffMs);
                continue;
            } catch (Exception e) {
                throw new IOException("TMDB request failed unexpectedly", e);
            }

            if (response.code() != 429) {
                return response;
            }

            long retryAfterMs = parseRetryAfterMillis(response);
            if (retryAfterMs < 0L) {
                retryAfterMs = BASE_RETRY_DELAY_MS * (attempt + 1L);
            }

            if (response.errorBody() != null) {
                response.errorBody().close();
            }
            if (attempt == MAX_429_RETRIES) {
                Log.w(TAG, "TMDB rate limited after max retries.");
                return response;
            }

            Log.w(TAG, String.format("TMDB rate limited (429), retrying in %dms", retryAfterMs));
            sleep(retryAfterMs);
        }

        throw new IOException("Unreachable TMDB retry state");
    }

    public List<Show> searchShows(String query, String language) {
        try {
            final Response<TvShowResultsPage> response = executeWithRetry(
                    () -> this.tmdb.searchService().tv(query, null, language, null, false).execute()
            );
            final TvShowResultsPage results = bodyOrNull(response);
            if (results != null) {
                final SearchShowsParser parser = new SearchShowsParser();
                return parser.parse(results, language);
            } else {
                return new LinkedList<>();
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            return new LinkedList<>();
        }
    }

    public Show getShow(HashMap<String, String> showIds, String language) {
        Show show = null;
        try {
            TvShow lookupResult = null;
            AppendToResponse includes = new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS);

            if (showIds.get("tmdbId") != null) {
                int tmdbId = Integer.parseInt(showIds.get("tmdbId"));
                Response<TvShow> seriesResponse = executeWithRetry(
                        () -> this.tmdb.tvService().tv(tmdbId, language, includes).execute()
                );
                lookupResult = bodyOrNull(seriesResponse);
            }

            if (lookupResult == null && showIds.get("tvdbId") != null) {
                Response<FindResults> seriesResponse = executeWithRetry(
                        () -> this.tmdb.findService().find(
                                showIds.get("tvdbId"),
                                ExternalSource.TVDB_ID,
                                language
                        ).execute()
                );
                FindResults findResults = bodyOrNull(seriesResponse);
                if (findResults != null) {
                    if (findResults.tv_results != null && findResults.tv_results.size() > 0) {
                        BaseTvShow sparseShow = findResults.tv_results.get(0);
                        Response<TvShow> showResponse = executeWithRetry(
                                () -> tmdb.tvService().tv(sparseShow.id, language, includes).execute()
                        );
                        lookupResult = bodyOrNull(showResponse);
                    }
                }
            }

            if (lookupResult == null && showIds.get("imdbId") != null) {
                Response<FindResults> seriesResponse = executeWithRetry(
                        () -> this.tmdb.findService().find(
                                showIds.get("imdbId"),
                                ExternalSource.IMDB_ID,
                                language
                        ).execute()
                );
                FindResults findResults = bodyOrNull(seriesResponse);
                if (findResults != null) {
                    if (findResults.tv_results != null && findResults.tv_results.size() > 0) {
                        BaseTvShow sparseShow = findResults.tv_results.get(0);
                        Response<TvShow> showResponse = executeWithRetry(
                                () -> tmdb.tvService().tv(sparseShow.id, language, includes).execute()
                        );
                        lookupResult = bodyOrNull(showResponse);
                    }
                }
            }

            if (lookupResult != null) {
                final GetShowParser parser = new GetShowParser();
                show = parser.parse(lookupResult, language);
                show.setEpisodes(getEpisodesForShow(lookupResult, language));
            }

        } catch (IOException e) {
            Log.w(TAG, e);
        }
        return show;
    }

    public Show getShow(int id, String language, boolean includeEpisodes) {
        try {
            AppendToResponse includes = new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS);
            Response<TvShow> seriesResponse = executeWithRetry(
                    () -> this.tmdb.tvService().tv(id, language, includes).execute()
            );
            Log.d(TAG, String.format("Received response %d: %s", seriesResponse.code(), seriesResponse.message()));
            final TvShow series = bodyOrNull(seriesResponse);
            if (series != null) {
                final GetShowParser parser = new GetShowParser();
                Show show = parser.parse(series, language);

                if (show != null && includeEpisodes) {
                    ArrayList<Episode> episodes = getEpisodesForShow(series, language);
                    show.setEpisodes(episodes);
                }
                return show;
            } else {
                return null;
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    public ArrayList<Episode> getEpisodesForShow(TvShow series, String language) {
        int episode_count = series.number_of_episodes != null ? series.number_of_episodes : 64;
        ArrayList<Episode> episodes = new ArrayList<>(episode_count);
        final GetEpisodesParser episodesParser = new GetEpisodesParser();
        if (series.number_of_seasons != null) {
            for (TvSeason season : series.seasons) {
                try {
                    AppendToResponse includes = new AppendToResponse(AppendToResponseItem.EXTERNAL_IDS);
                    final int seasonNumber = season.season_number;
                    Response<TvSeason> seasonResponse = executeWithRetry(
                            () -> this.tmdb.tvSeasonsService()
                                    .season(series.id, seasonNumber, language, includes)
                                    .execute()
                    );
                    season = bodyOrNull(seasonResponse);
                    if (season != null) {
                        episodes.addAll(episodesParser.parse(season.episodes));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return episodes;
    }
}
