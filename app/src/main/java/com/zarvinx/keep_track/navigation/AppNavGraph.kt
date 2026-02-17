package com.zarvinx.keep_track.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zarvinx.keep_track.AddShowPreviewScaffold
import com.zarvinx.keep_track.AddShowSearchScaffold
import com.zarvinx.keep_track.AboutScreen
import com.zarvinx.keep_track.BackupSettingsRoute
import com.zarvinx.keep_track.EpisodeScreen
import com.zarvinx.keep_track.MainScreen
import com.zarvinx.keep_track.SeasonScreen
import com.zarvinx.keep_track.SettingsScreen
import com.zarvinx.keep_track.refreshAllShows
import com.zarvinx.keep_track.ui.ShowDetailScreen
import com.zarvinx.keep_track.ui.ShowsViewModel

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.Main.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 150))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 150))
        }
    ) {
        composable(route = AppDestination.Main.route) {
            val viewModel: ShowsViewModel = hiltViewModel()
            MainScreen(
                viewModel = viewModel,
                onShowSelected = { showId ->
                    navController.navigate(AppDestination.Show.createRoute(showId))
                },
                onSettings = {
                    navController.navigate(AppDestination.Settings.route)
                },
                onAbout = {
                    navController.navigate(AppDestination.About.route)
                },
                onRefreshAll = {
                    refreshAllShows()
                },
                onAddShow = {
                    navController.navigate(AppDestination.AddShowSearch.createRoute(null))
                }
            )
        }

        composable(route = AppDestination.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onBackupSettings = { navController.navigate(AppDestination.BackupSettings.route) }
            )
        }

        composable(route = AppDestination.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = AppDestination.BackupSettings.route) {
            BackupSettingsRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppDestination.Show.route,
            arguments = listOf(
                navArgument(AppDestination.Show.ARG_SHOW_ID) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getInt(AppDestination.Show.ARG_SHOW_ID) ?: -1
            if (showId < 0) {
                InvalidNavigationArgumentsScreen {
                    popBackOrMain(navController)
                }
                return@composable
            }

            ShowDetailScreen(
                showId = showId,
                onNavigateBack = { navController.popBackStack() },
                onSeasonSelected = { seasonNumber ->
                    navController.navigate(AppDestination.Season.createRoute(showId, seasonNumber))
                }
            )
        }

        composable(
            route = AppDestination.Season.route,
            arguments = listOf(
                navArgument(AppDestination.Season.ARG_SHOW_ID) { type = NavType.IntType },
                navArgument(AppDestination.Season.ARG_SEASON_NUMBER) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getInt(AppDestination.Season.ARG_SHOW_ID) ?: -1
            val seasonNumber = backStackEntry.arguments?.getInt(AppDestination.Season.ARG_SEASON_NUMBER) ?: -1
            if (showId < 0 || seasonNumber < 0) {
                InvalidNavigationArgumentsScreen {
                    popBackOrMain(navController)
                }
                return@composable
            }

            SeasonScreen(
                showId = showId,
                seasonNumber = seasonNumber,
                onNavigateBack = { navController.popBackStack() },
                onEpisodeSelected = { episodeId ->
                    navController.navigate(
                        AppDestination.Episode.createRoute(showId, seasonNumber, episodeId)
                    )
                }
            )
        }

        composable(
            route = AppDestination.Episode.route,
            arguments = listOf(
                navArgument(AppDestination.Episode.ARG_SHOW_ID) { type = NavType.IntType },
                navArgument(AppDestination.Episode.ARG_SEASON_NUMBER) { type = NavType.IntType },
                navArgument(AppDestination.Episode.ARG_INITIAL_EPISODE_ID) { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getInt(AppDestination.Episode.ARG_SHOW_ID) ?: -1
            val seasonNumber = backStackEntry.arguments?.getInt(AppDestination.Episode.ARG_SEASON_NUMBER) ?: -1
            val initialEpisodeId = backStackEntry.arguments
                ?.getInt(AppDestination.Episode.ARG_INITIAL_EPISODE_ID) ?: -1
            if (showId < 0 || seasonNumber < 0 || initialEpisodeId < 0) {
                InvalidNavigationArgumentsScreen {
                    popBackOrMain(navController)
                }
                return@composable
            }

            EpisodeScreen(
                showId = showId,
                seasonNumber = seasonNumber,
                episodeId = initialEpisodeId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = AppDestination.AddShowSearch.route,
            arguments = listOf(
                navArgument(AppDestination.AddShowSearch.ARG_QUERY) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val query = backStackEntry.arguments
                ?.getString(AppDestination.AddShowSearch.ARG_QUERY)
                .orEmpty()
            AddShowSearchScaffold(
                query = query,
                onNavigateBack = { navController.popBackStack() },
                onShowClick = { args ->
                    navController.navigate(AppDestination.AddShowPreview.createRoute(args))
                }
            )
        }

        composable(
            route = AppDestination.AddShowPreview.route,
            arguments = listOf(
                navArgument(AppDestination.AddShowPreview.ARG_PREVIEW_ARGS) {
                    type = AddShowPreviewArgsNavType
                }
            )
        ) { backStackEntry ->
            val previewArgs = backStackEntry.arguments?.let {
                AddShowPreviewArgsNavType.get(it, AppDestination.AddShowPreview.ARG_PREVIEW_ARGS)
            }
            if (previewArgs == null) {
                InvalidNavigationArgumentsScreen {
                    popBackOrMain(navController)
                }
                return@composable
            }

            AddShowPreviewScaffold(
                previewArgs = previewArgs,
                onNavigateBack = { navController.popBackStack() },
                onAddShow = {
                    navController.navigate(AppDestination.Main.route) {
                        popUpTo(AppDestination.Main.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

private fun popBackOrMain(navController: androidx.navigation.NavHostController) {
    val popped = navController.popBackStack()
    if (!popped) {
        navController.navigate(AppDestination.Main.route) {
            launchSingleTop = true
        }
    }
}

@Composable
private fun InvalidNavigationArgumentsScreen(onFallback: () -> Unit) {
    LaunchedEffect(Unit) {
        onFallback()
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Unable to open this screen.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
