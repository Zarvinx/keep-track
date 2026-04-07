package com.zarvinx.keep_track

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import coil.imageLoader
import com.zarvinx.keep_track.db.room.AppDatabase
import com.zarvinx.keep_track.db.room.EpisodeEntity
import com.zarvinx.keep_track.db.room.ShowEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonBackup {
    private const val TAG = "JsonBackup"
    private const val BACKUP_VERSION = 1

    // User-facing settings to include. Excludes runtime state and device-specific values.
    private val SETTINGS_KEYS = setOf(
        "pref_language",
        "pref_shows_filter",
        "pref_auto_refresh_enabled",
        "pref_auto_refresh_period",
        "pref_auto_refresh_wifi_only",
        AutoBackupHelper.KEY_PREF_AUTO_BACKUP_ENABLED,
        AutoBackupHelper.KEY_PREF_AUTO_BACKUP_PERIOD,
        AutoBackupHelper.KEY_PREF_AUTO_BACKUP_RETENTION,
    )

    fun exportToJson(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dao = AppDatabase.getInstance(context).backupDao()

        val root = JSONObject()
        root.put("backup_version", BACKUP_VERSION)
        root.put("exported_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))

        // Settings
        val settings = JSONObject()
        val allPrefs = prefs.all
        for (key in SETTINGS_KEYS) {
            val value = allPrefs[key] ?: continue
            when (value) {
                is Boolean -> settings.put(key, value)
                is Int -> settings.put(key, value)
                is Long -> settings.put(key, value)
                is Float -> settings.put(key, value.toDouble())
                is String -> settings.put(key, value)
            }
        }
        root.put("settings", settings)

        // Shows
        val shows = dao.getAllShows()
        val showsArray = JSONArray()
        for (show in shows) {
            showsArray.put(showToJson(show))
        }
        root.put("shows", showsArray)

        // Episodes
        val episodes = dao.getAllEpisodes()
        val episodesArray = JSONArray()
        for (episode in episodes) {
            episodesArray.put(episodeToJson(episode))
        }
        root.put("episodes", episodesArray)

        Log.i(TAG, "Exported ${shows.size} shows and ${episodes.size} episodes.")
        return root.toString(2)
    }

    fun importFromJson(context: Context, json: String) {
        val root = JSONObject(json)
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val dao = AppDatabase.getInstance(context).backupDao()

        // Settings — skip the backup dir URI since it's device-specific
        root.optJSONObject("settings")?.let { settings ->
            val editor = prefs.edit()
            val keys = settings.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key == FileUtilities.KEY_PREF_BACKUP_DIR_URI) continue
                when (val value = settings.get(key)) {
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                    is String -> editor.putString(key, value)
                }
            }
            editor.apply()
        }

        // Clear existing data (episodes first to avoid FK violations)
        dao.deleteAllEpisodes()
        dao.deleteAllShows()

        // Restore shows (preserving original IDs so episode show_id references remain valid)
        val showsArray = root.getJSONArray("shows")
        for (i in 0 until showsArray.length()) {
            dao.insertShow(showFromJson(showsArray.getJSONObject(i)))
        }

        // Restore episodes
        val episodesArray = root.getJSONArray("episodes")
        for (i in 0 until episodesArray.length()) {
            dao.insertEpisode(episodeFromJson(episodesArray.getJSONObject(i)))
        }

        Log.i(TAG, "Imported ${showsArray.length()} shows and ${episodesArray.length()} episodes.")
        context.imageLoader.diskCache?.clear()
    }

    private fun showToJson(show: ShowEntity): JSONObject {
        val obj = JSONObject()
        obj.put("id", show.id)
        obj.put("tvdb_id", show.tvdbId)
        obj.put("tmdb_id", show.tmdbId)
        obj.put("imdb_id", show.imdbId)
        obj.put("name", show.name)
        obj.put("language", show.language)
        obj.put("overview", show.overview)
        obj.put("first_aired", show.firstAired)
        obj.put("starred", show.starred)
        obj.put("archived", show.archived)
        obj.put("banner_path", show.bannerPath)
        obj.put("fanart_path", show.fanartPath)
        obj.put("poster_path", show.posterPath)
        obj.put("notes", show.notes)
        obj.put("status", show.status)
        return obj
    }

    private fun episodeToJson(episode: EpisodeEntity): JSONObject {
        val obj = JSONObject()
        obj.put("id", episode.id)
        obj.put("tvdb_id", episode.tvdbId)
        obj.put("tmdb_id", episode.tmdbId)
        obj.put("imdb_id", episode.imdbId)
        obj.put("show_id", episode.showId)
        obj.put("name", episode.name)
        obj.put("language", episode.language)
        obj.put("overview", episode.overview)
        obj.put("episode_number", episode.episodeNumber)
        obj.put("season_number", episode.seasonNumber)
        obj.put("first_aired", episode.firstAired)
        obj.put("watched", episode.watched)
        return obj
    }

    private fun showFromJson(obj: JSONObject): ShowEntity {
        return ShowEntity(
            id = obj.optInt("id").takeIf { !obj.isNull("id") },
            tvdbId = obj.optInt("tvdb_id").takeIf { !obj.isNull("tvdb_id") },
            tmdbId = obj.optInt("tmdb_id").takeIf { !obj.isNull("tmdb_id") },
            imdbId = obj.optString("imdb_id").takeIf { !obj.isNull("imdb_id") },
            name = obj.getString("name"),
            language = obj.optString("language").takeIf { !obj.isNull("language") },
            overview = obj.optString("overview").takeIf { !obj.isNull("overview") },
            firstAired = obj.optLong("first_aired").takeIf { !obj.isNull("first_aired") },
            starred = obj.optInt("starred").takeIf { !obj.isNull("starred") },
            archived = obj.optInt("archived").takeIf { !obj.isNull("archived") },
            bannerPath = obj.optString("banner_path").takeIf { !obj.isNull("banner_path") },
            fanartPath = obj.optString("fanart_path").takeIf { !obj.isNull("fanart_path") },
            posterPath = obj.optString("poster_path").takeIf { !obj.isNull("poster_path") },
            notes = obj.optString("notes").takeIf { !obj.isNull("notes") },
            status = obj.optString("status").takeIf { !obj.isNull("status") }
        )
    }

    private fun episodeFromJson(obj: JSONObject): EpisodeEntity {
        return EpisodeEntity(
            id = obj.optInt("id"),
            tvdbId = obj.optInt("tvdb_id").takeIf { !obj.isNull("tvdb_id") },
            tmdbId = obj.optInt("tmdb_id").takeIf { !obj.isNull("tmdb_id") },
            imdbId = obj.optString("imdb_id").takeIf { !obj.isNull("imdb_id") },
            showId = obj.getInt("show_id"),
            name = obj.getString("name"),
            language = obj.optString("language").takeIf { !obj.isNull("language") },
            overview = obj.optString("overview").takeIf { !obj.isNull("overview") },
            episodeNumber = obj.optInt("episode_number").takeIf { !obj.isNull("episode_number") },
            seasonNumber = obj.optInt("season_number").takeIf { !obj.isNull("season_number") },
            firstAired = obj.optLong("first_aired").takeIf { !obj.isNull("first_aired") },
            watched = obj.optInt("watched").takeIf { !obj.isNull("watched") }
        )
    }
}
