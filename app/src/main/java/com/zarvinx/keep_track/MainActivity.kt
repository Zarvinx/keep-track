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

package com.zarvinx.keep_track

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import com.zarvinx.keep_track.navigation.AppNavGraph
import com.zarvinx.keep_track.services.AsyncTask
import com.zarvinx.keep_track.services.RefreshAllShowsTask
import com.zarvinx.keep_track.ui.ShowsListScreen
import com.zarvinx.keep_track.ui.ShowsViewModel
import com.zarvinx.keep_track.ui.theme.BackgroundGradientOption
import com.zarvinx.keep_track.ui.theme.KeepTrackTheme
import com.zarvinx.keep_track.ui.theme.defaultCustomBackgroundEnd
import com.zarvinx.keep_track.ui.theme.defaultCustomBackgroundStart
import com.zarvinx.keep_track.ui.theme.findBackgroundGradientOption
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

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
            KeepTrackTheme {
                AppNavGraph()
            }
        }
    }
}

fun refreshAllShows() {
    AsyncTask().executeAsync(
        RefreshAllShowsTask(),
        onError = {
            MainActivity.getAppContext()?.let { context ->
                Toast.makeText(context, context.getString(R.string.refresh_all_error_message), Toast.LENGTH_LONG).show()
            }
        }
    )
    MainActivity.getAppContext()?.let { context ->
        Toast.makeText(context, context.getString(R.string.refreshing_all_shows_background), Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ShowsViewModel,
    onShowSelected: (Int) -> Unit,
    onSettings: () -> Unit,
    onRefreshAll: () -> Unit,
    onAddShow: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val state = rememberMainScreenState(viewModel)
    val currentFilter by viewModel.currentFilter.collectAsState()
    val selectedGradient = rememberSelectedBackgroundGradient()
    val emptyStateTextColor = if (
        lerp(selectedGradient.startColor, selectedGradient.endColor, 0.5f).luminance() > 0.45f
    ) Color(0xFF202124) else Color.White
    var showFilters by remember { mutableStateOf(false) }

    val filterMenuItems = remember {
        listOf(
            FilterMenuItem(R.string.menu_filter_uncompleted, ShowsViewModel.SHOWS_FILTER_WATCHING),
            FilterMenuItem(R.string.menu_filter_starred, ShowsViewModel.SHOWS_FILTER_STARRED),
            FilterMenuItem(R.string.menu_filter_archived, ShowsViewModel.SHOWS_FILTER_ARCHIVED),
            FilterMenuItem(R.string.menu_filter_upcoming, ShowsViewModel.SHOWS_FILTER_UPCOMING),
            FilterMenuItem(R.string.menu_filter_all, ShowsViewModel.SHOWS_FILTER_ALL)
        )
    }
    val currentFilterLabel = stringResource(
        filterMenuItems.firstOrNull { it.filterValue == currentFilter }?.labelResId
            ?: R.string.menu_filter_uncompleted
    )

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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hazeState = rememberHazeState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(
                selectedGradient.startColor,
                selectedGradient.endColor
            )))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (state.isSearching) {
                    MainTopBar(
                        state = state,
                        focusRequester = focusRequester,
                        keyboardController = keyboardController
                    )
                }
            },
            bottomBar = {
                MainBottomBar(
                    currentFilterLabel = currentFilterLabel,
                    isSearching = state.isSearching,
                    onFilters = { showFilters = true },
                    onRefresh = onRefreshAll,
                    onSearch = {
                        if (state.isSearching) state.closeSearch(keyboardController)
                        else state.startSearch()
                    },
                    onAdd = onAddShow,
                    onSettings = onSettings
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
                ShowsListScreen(
                    viewModel = viewModel,
                    hazeState = hazeState,
                    contentPadding = paddingValues,
                    emptyStateTextColor = emptyStateTextColor,
                    onShowClick = onShowSelected
                )
            }
        }
    }

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            Text(
                text = stringResource(R.string.menu_filter_shows_list),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
            )
            filterMenuItems.forEach { item ->
                val selected = currentFilter == item.filterValue
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PreferenceManager.getDefaultSharedPreferences(context)
                                .edit()
                                .putInt(ShowsViewModel.KEY_PREF_SHOWS_FILTER, item.filterValue)
                                .apply()
                            showFilters = false
                        }
                        .height(56.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (item.filterValue) {
                        ShowsViewModel.SHOWS_FILTER_WATCHING -> Icons.Default.PlayArrow
                        ShowsViewModel.SHOWS_FILTER_STARRED -> Icons.Default.Star
                        ShowsViewModel.SHOWS_FILTER_ARCHIVED -> Icons.Default.Archive
                        ShowsViewModel.SHOWS_FILTER_UPCOMING -> Icons.Default.DateRange
                        else -> Icons.Default.List
                    }
                    Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = stringResource(item.labelResId),
                        modifier = Modifier.weight(1f).padding(start = 20.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MainBottomBar(
    currentFilterLabel: String,
    isSearching: Boolean,
    onFilters: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = colorScheme.onSurface.copy(alpha = 0.08f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomAction(
                    icon = Icons.Default.FilterList,
                    label = currentFilterLabel,
                    accessibilityLabel = stringResource(R.string.menu_filter_shows_list) + ": " + currentFilterLabel,
                    onClick = onFilters,
                    selected = true,
                    modifier = Modifier.weight(1f)
                )
                BottomAction(
                    icon = Icons.Default.Refresh,
                    label = stringResource(R.string.bottom_refresh),
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                )
                BottomAction(
                    icon = Icons.Default.Search,
                    label = stringResource(R.string.bottom_search),
                    onClick = onSearch,
                    selected = isSearching,
                    modifier = Modifier.weight(1f)
                )
                BottomAction(
                    icon = Icons.Default.Add,
                    label = stringResource(R.string.bottom_add),
                    onClick = onAdd,
                    emphasized = true,
                    modifier = Modifier.weight(1f)
                )
                BottomAction(
                    icon = Icons.Default.Settings,
                    label = stringResource(R.string.menu_settings),
                    onClick = onSettings,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    emphasized: Boolean = false,
    accessibilityLabel: String = label
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .semantics { contentDescription = accessibilityLabel }
            .clickable(onClick = onClick)
            .height(58.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(if (emphasized) 34.dp else 28.dp)
                .background(if (emphasized) colors.primary else Color.Transparent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    emphasized -> colors.onPrimary
                    selected -> colors.primary
                    else -> colors.onSurfaceVariant
                },
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun rememberSelectedBackgroundGradient(): BackgroundGradientOption {
    val context = LocalContext.current
    val prefs = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    var selectedId by remember {
        mutableStateOf(
            prefs.getString(
                Preferences.KEY_PREF_BACKGROUND_GRADIENT,
                Preferences.BACKGROUND_GRADIENT_MIST_BLUE
            ) ?: Preferences.BACKGROUND_GRADIENT_MIST_BLUE
        )
    }
    var customStartHex by remember {
        mutableStateOf(
            prefs.getString(
                Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_START,
                "#F5F5F5"
            ) ?: "#F5F5F5"
        )
    }
    var customEndHex by remember {
        mutableStateOf(
            prefs.getString(
                Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_END,
                "#2A456F"
            ) ?: "#2A456F"
        )
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
                Preferences.KEY_PREF_BACKGROUND_GRADIENT -> {
                    selectedId = sharedPrefs.getString(
                        Preferences.KEY_PREF_BACKGROUND_GRADIENT,
                        Preferences.BACKGROUND_GRADIENT_MIST_BLUE
                    ) ?: Preferences.BACKGROUND_GRADIENT_MIST_BLUE
                }
                Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_START -> {
                    customStartHex = sharedPrefs.getString(
                        Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_START,
                        customStartHex
                    ) ?: customStartHex
                }
                Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_END -> {
                    customEndHex = sharedPrefs.getString(
                        Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_END,
                        customEndHex
                    ) ?: customEndHex
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val customStart = parseColorHexOrDefault(customStartHex, defaultCustomBackgroundStart())
    val customEnd = parseColorHexOrDefault(customEndHex, defaultCustomBackgroundEnd())
    return findBackgroundGradientOption(selectedId, customStart, customEnd)
}

private fun parseColorHexOrDefault(hex: String, defaultColor: Color): Color {
    val normalized = hex.removePrefix("#")
    if (normalized.length != 6) return defaultColor
    return try {
        val intValue = normalized.toLong(16).toInt()
        Color(
            red = ((intValue shr 16) and 0xFF) / 255f,
            green = ((intValue shr 8) and 0xFF) / 255f,
            blue = (intValue and 0xFF) / 255f
        )
    } catch (_: NumberFormatException) {
        defaultColor
    }
}
