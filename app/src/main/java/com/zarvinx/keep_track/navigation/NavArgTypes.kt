package com.zarvinx.keep_track.navigation

import android.net.Uri
import android.os.Bundle
import androidx.navigation.NavType
import com.zarvinx.keep_track.ui.AddShowPreviewArgs
import org.json.JSONObject

private const val KEY_TMDB_ID = "tmdbId"
private const val KEY_NAME = "name"
private const val KEY_LANGUAGE = "language"
private const val KEY_OVERVIEW = "overview"
private const val KEY_FIRST_AIRED_MILLIS = "firstAiredMillis"

/**
 * Custom [NavType] for transporting [AddShowPreviewArgs] through navigation routes.
 */
object AddShowPreviewArgsNavType : NavType<AddShowPreviewArgs>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): AddShowPreviewArgs? {
        val raw = bundle.getString(key) ?: return null
        return decodePreviewArgs(raw)
    }

    override fun parseValue(value: String): AddShowPreviewArgs {
        return decodePreviewArgs(value)
            ?: throw IllegalArgumentException("Invalid preview args")
    }

    override fun put(bundle: Bundle, key: String, value: AddShowPreviewArgs) {
        bundle.putString(key, serializePreviewArgs(value))
    }
}

/**
 * Serializes and URL-encodes preview args for route-safe transport.
 */
fun encodePreviewArgs(args: AddShowPreviewArgs): String {
    return Uri.encode(serializePreviewArgs(args))
}

/**
 * Decodes preview args from either encoded or raw JSON route value.
 *
 * @return parsed args, or null when decoding/parsing fails.
 */
fun decodePreviewArgs(value: String): AddShowPreviewArgs? {
    val jsonText = runCatching { Uri.decode(value) }.getOrElse { value }
    return runCatching {
        val json = JSONObject(jsonText)
        AddShowPreviewArgs(
            tmdbId = json.getInt(KEY_TMDB_ID),
            name = json.getString(KEY_NAME),
            language = json.getString(KEY_LANGUAGE),
            overview = json.optString(KEY_OVERVIEW, ""),
            firstAiredMillis = if (json.isNull(KEY_FIRST_AIRED_MILLIS)) {
                null
            } else {
                json.getLong(KEY_FIRST_AIRED_MILLIS)
            }
        )
    }.getOrNull()
}

private fun serializePreviewArgs(args: AddShowPreviewArgs): String {
    return JSONObject().apply {
        put(KEY_TMDB_ID, args.tmdbId)
        put(KEY_NAME, args.name)
        put(KEY_LANGUAGE, args.language)
        put(KEY_OVERVIEW, args.overview)
        if (args.firstAiredMillis == null) {
            put(KEY_FIRST_AIRED_MILLIS, JSONObject.NULL)
        } else {
            put(KEY_FIRST_AIRED_MILLIS, args.firstAiredMillis)
        }
    }.toString()
}
