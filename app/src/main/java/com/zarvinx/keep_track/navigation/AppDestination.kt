package com.zarvinx.keep_track.navigation

import android.net.Uri
import com.zarvinx.keep_track.ui.AddShowPreviewArgs

/**
 * Typed navigation destination contract for top-level app routes.
 */
sealed class AppDestination(val route: String) {
    data object Main : AppDestination("main")
    data object Settings : AppDestination("settings")
    data object About : AppDestination("about")
    data object BackupSettings : AppDestination("backupSettings")

    data object Show : AppDestination("show/{showId}") {
        const val ARG_SHOW_ID = "showId"

        /**
         * Builds concrete route for a show details screen.
         */
        fun createRoute(showId: Int): String = "show/$showId"
    }

    data object Season : AppDestination("season/{showId}/{seasonNumber}") {
        const val ARG_SHOW_ID = "showId"
        const val ARG_SEASON_NUMBER = "seasonNumber"

        /**
         * Builds concrete route for a season episode list.
         */
        fun createRoute(showId: Int, seasonNumber: Int): String =
            "season/$showId/$seasonNumber"
    }

    data object Episode : AppDestination("episode/{showId}/{seasonNumber}/{initialEpisodeId}") {
        const val ARG_SHOW_ID = "showId"
        const val ARG_SEASON_NUMBER = "seasonNumber"
        const val ARG_INITIAL_EPISODE_ID = "initialEpisodeId"

        /**
         * Builds concrete route for episode pager screen.
         */
        fun createRoute(showId: Int, seasonNumber: Int, initialEpisodeId: Int): String =
            "episode/$showId/$seasonNumber/$initialEpisodeId"
    }

    data object AddShowSearch : AppDestination("addShowSearch?query={query}") {
        const val ARG_QUERY = "query"

        /**
         * Builds add-show search route with optional prefilled query.
         */
        fun createRoute(query: String?): String {
            if (query.isNullOrBlank()) return "addShowSearch"
            return "addShowSearch?query=${Uri.encode(query)}"
        }
    }

    data object AddShowPreview : AppDestination("addShowPreview/{previewArgs}") {
        const val ARG_PREVIEW_ARGS = "previewArgs"

        /**
         * Builds preview route using encoded preview arguments payload.
         */
        fun createRoute(previewArgs: AddShowPreviewArgs): String {
            return "addShowPreview/${encodePreviewArgs(previewArgs)}"
        }
    }
}
