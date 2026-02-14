/*
 * Copyright (C) 2012-2014 Jamie Nicol <jamie@thenicols.net>
 * Copyright (C) 2026 Zarvinx (Kotlin/Compose conversion)
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

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.navigation.AppNavGraph
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.RefreshAllShowsTask
import com.redcoracle.episodes.ui.ShowsListScreen
import com.redcoracle.episodes.ui.ShowsViewModel
import com.redcoracle.episodes.ui.theme.EpisodesTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

data class MainMenuItem(
    val labelResId: Int,
    val closeDrawerBeforeAction: Boolean = true,
    val action: () -> Unit
)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private var context: Context? = null

        @JvmStatic
        fun getAppContext(): Context? = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext
        AutoRefreshHelper.getInstance(applicationContext).rescheduleAlarm()

        // Hide the default action bar to prevent double title bars
        supportActionBar?.hide()

        setContent {
            EpisodesTheme {
                AppNavGraph()
            }
        }
    }
}

fun refreshAllShows() {
    AsyncTask().executeAsync(RefreshAllShowsTask())
    MainActivity.getAppContext()?.let { context ->
        Toast.makeText(context, "Refreshing all shows in background...", Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    viewModel: ShowsViewModel,
    onShowSelected: (Int) -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onRefreshAll: () -> Unit,
    onAddShow: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val state = rememberMainScreenState(viewModel)
    val currentFilter by viewModel.currentFilter.collectAsState()
    val drawerState = androidx.compose.material3.rememberDrawerState(
        initialValue = androidx.compose.material3.DrawerValue.Closed
    )

    val filterMenuItems = androidx.compose.runtime.remember {
        listOf(
            FilterMenuItem(R.string.menu_filter_uncompleted, ShowsViewModel.SHOWS_FILTER_WATCHING),
            FilterMenuItem(R.string.menu_filter_starred, ShowsViewModel.SHOWS_FILTER_STARRED),
            FilterMenuItem(R.string.menu_filter_archived, ShowsViewModel.SHOWS_FILTER_ARCHIVED),
            FilterMenuItem(R.string.menu_filter_upcoming, ShowsViewModel.SHOWS_FILTER_UPCOMING),
            FilterMenuItem(R.string.menu_filter_all, ShowsViewModel.SHOWS_FILTER_ALL)
        )
    }

    val mainMenuItems = androidx.compose.runtime.remember(onRefreshAll, onSettings, onAbout) {
        listOf(
            MainMenuItem(
                labelResId = R.string.menu_refresh_all_shows,
                action = onRefreshAll
            ),
            MainMenuItem(
                labelResId = R.string.menu_settings,
                closeDrawerBeforeAction = false,
                action = onSettings
            ),
            MainMenuItem(
                labelResId = R.string.menu_about,
                closeDrawerBeforeAction = false,
                action = onAbout
            )
        )
    }

    fun applyFilter(filter: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putInt(ShowsViewModel.KEY_PREF_SHOWS_FILTER, filter).apply()
    }

    LaunchedEffect(state.isSearching) {
        if (state.isSearching) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BackHandler(enabled = state.isSearching) {
        state.closeSearch(keyboardController)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                state.closeSearch(keyboardController)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        androidx.compose.material3.Surface(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Text(
                                text = context.getString(R.string.menu_filter_shows_list),
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                        filterMenuItems.forEach { item ->
                            androidx.compose.material3.NavigationDrawerItem(
                                label = {
                                    androidx.compose.material3.Text(
                                        text = context.getString(item.labelResId),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                    )
                                },
                                selected = currentFilter == item.filterValue,
                                onClick = {
                                    applyFilter(item.filterValue)
                                    scope.launch { drawerState.close() }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                    .height(42.dp)
                            )
                        }
                    }

                    Column {
                        androidx.compose.material3.Divider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )

                        mainMenuItems.forEach { item ->
                            androidx.compose.material3.NavigationDrawerItem(
                                label = {
                                    androidx.compose.material3.Text(
                                        text = context.getString(item.labelResId),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                                    )
                                },
                                selected = false,
                                onClick = {
                                    if (item.closeDrawerBeforeAction) {
                                        scope.launch {
                                            drawerState.close()
                                            item.action()
                                        }
                                    } else {
                                        item.action()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                                    .height(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    ) {
        androidx.compose.material3.Scaffold(
            topBar = {
                MainTopBar(
                    state = state,
                    focusRequester = focusRequester,
                    keyboardController = keyboardController,
                    onAddShow = onAddShow,
                    onOpenFiltersDrawer = {
                        scope.launch { drawerState.open() }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ShowsListScreen(
                    viewModel = viewModel,
                    onShowClick = onShowSelected
                )
            }
        }
    }

}
