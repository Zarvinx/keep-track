package com.redcoracle.episodes.navigation

import android.net.Uri
import com.redcoracle.episodes.ui.AddShowPreviewArgs

sealed class AppDestination(val route: String) {
    data object Main : AppDestination("main")

    data object Show : AppDestination("show/{showId}") {
        const val ARG_SHOW_ID = "showId"
        fun createRoute(showId: Int): String = "show/$showId"
    }

    data object Season : AppDestination("season/{showId}/{seasonNumber}") {
        const val ARG_SHOW_ID = "showId"
        const val ARG_SEASON_NUMBER = "seasonNumber"

        fun createRoute(showId: Int, seasonNumber: Int): String =
            "season/$showId/$seasonNumber"
    }

    data object Episode : AppDestination("episode/{showId}/{seasonNumber}/{initialEpisodeId}") {
        const val ARG_SHOW_ID = "showId"
        const val ARG_SEASON_NUMBER = "seasonNumber"
        const val ARG_INITIAL_EPISODE_ID = "initialEpisodeId"

        fun createRoute(showId: Int, seasonNumber: Int, initialEpisodeId: Int): String =
            "episode/$showId/$seasonNumber/$initialEpisodeId"
    }

    data object AddShowSearch : AppDestination("addShowSearch?query={query}") {
        const val ARG_QUERY = "query"

        fun createRoute(query: String?): String {
            if (query.isNullOrBlank()) return "addShowSearch"
            return "addShowSearch?query=${Uri.encode(query)}"
        }
    }

    data object AddShowPreview : AppDestination("addShowPreview/{previewArgs}") {
        const val ARG_PREVIEW_ARGS = "previewArgs"

        fun createRoute(previewArgs: AddShowPreviewArgs): String {
            return "addShowPreview/${encodePreviewArgs(previewArgs)}"
        }
    }
}
