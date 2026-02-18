package com.zarvinx.keep_track.db.room

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EpisodeWatchStateWriterTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setUp() {
        AppDatabase.closeInstance()
        context.deleteDatabase(AppDatabaseFile.resolveDbName())
    }

    @After
    fun tearDown() {
        AppDatabase.closeInstance()
        context.deleteDatabase(AppDatabaseFile.resolveDbName())
    }

    @Test
    fun setSeasonWatched_marksOnlyAlreadyAiredEpisodes() {
        val db = AppDatabase.getInstance(context)
        val sqlDb = db.openHelper.writableDatabase

        val showId = 1001
        val nowSeconds = System.currentTimeMillis() / 1000
        val airedEpisodeTime = nowSeconds - 86_400
        val futureEpisodeTime = nowSeconds + 86_400

        sqlDb.execSQL(
            "INSERT INTO shows (_id, tmdb_id, name, language, starred, archived) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf(showId, 1001, "Test Show", "en", 0, 0)
        )

        sqlDb.execSQL(
            "INSERT INTO episodes (_id, show_id, name, season_number, episode_number, first_aired, watched) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf(2001, showId, "Aired episode", 1, 1, airedEpisodeTime, 0)
        )
        sqlDb.execSQL(
            "INSERT INTO episodes (_id, show_id, name, season_number, episode_number, first_aired, watched) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf(2002, showId, "Future episode", 1, 2, futureEpisodeTime, 0)
        )

        val writer = EpisodeWatchStateWriter(context)
        kotlinx.coroutines.runBlocking {
            writer.setSeasonWatched(showId = showId, seasonNumber = 1, watched = true)
        }

        val episodes = db.appReadDao().getEpisodesForSeason(showId, 1)
        val watchedByEpisodeNumber = episodes.associate { it.episodeNumber to (it.watched ?: 0) }

        assertEquals(1, watchedByEpisodeNumber[1])
        assertEquals(0, watchedByEpisodeNumber[2])
    }
}
