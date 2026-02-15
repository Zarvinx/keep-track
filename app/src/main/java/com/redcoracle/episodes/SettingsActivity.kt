/*
 * Copyright (C) 2014 Jamie Nicol <jamie@thenicols.net>
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

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.settings.AccentColorSelectionDialog
import com.redcoracle.episodes.settings.BackgroundGradientSelectionDialog
import com.redcoracle.episodes.settings.accentOptionLabel
import com.redcoracle.episodes.settings.backgroundGradientLabel
import com.redcoracle.episodes.settings.buildAccentColorOptions
import com.redcoracle.episodes.settings.buildBackgroundGradientOptions
import com.redcoracle.episodes.settings.colorToHex
import com.redcoracle.episodes.settings.CustomGradientColorPickerDialog
import com.redcoracle.episodes.settings.readInitialAccentColorsMode
import com.redcoracle.episodes.settings.readInitialBackgroundGradient
import com.redcoracle.episodes.settings.readInitialCustomBackgroundEndHex
import com.redcoracle.episodes.settings.readInitialCustomBackgroundStartHex
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.RefreshAllShowsTask

private object SettingsRefreshScheduler {
    private const val LANGUAGE_REFRESH_DEBOUNCE_MS = 5_000L
    private val languageRefreshHandler = Handler(Looper.getMainLooper())
    private var pendingLanguageRefresh: Runnable? = null

    fun scheduleLanguageRefresh() {
        pendingLanguageRefresh?.let(languageRefreshHandler::removeCallbacks)
        pendingLanguageRefresh = Runnable {
            AsyncTask().executeAsync(RefreshAllShowsTask())
            pendingLanguageRefresh = null
        }.also {
            languageRefreshHandler.postDelayed(it, LANGUAGE_REFRESH_DEBOUNCE_MS)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onBackupSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val languageEntries = context.resources.getStringArray(R.array.language_entries)
    val languageValues = context.resources.getStringArray(R.array.language_values)
    val periodEntries = context.resources.getStringArray(R.array.auto_refresh_period_entries)
    val periodValues = context.resources.getStringArray(R.array.auto_refresh_period_values)

    var selectedLanguage by remember { mutableStateOf(prefs.getString("pref_language", "en") ?: "en") }
    var autoRefreshEnabled by remember { mutableStateOf(prefs.getBoolean("pref_auto_refresh_enabled", false)) }
    var autoRefreshPeriod by remember { mutableStateOf(prefs.getString("pref_auto_refresh_period", "168") ?: "168") }
    var autoRefreshWifiOnly by remember { mutableStateOf(prefs.getBoolean("pref_auto_refresh_wifi_only", true)) }
    var selectedThemeMode by remember {
        mutableStateOf(
            prefs.getString(Preferences.KEY_PREF_THEME_MODE, Preferences.THEME_MODE_SYSTEM)
                ?: Preferences.THEME_MODE_SYSTEM
        )
    }
    var customGradientStartHex by remember { mutableStateOf(readInitialCustomBackgroundStartHex(prefs)) }
    var customGradientEndHex by remember { mutableStateOf(readInitialCustomBackgroundEndHex(prefs)) }
    val backgroundOptions = buildBackgroundGradientOptions(customGradientStartHex, customGradientEndHex)
    var selectedBackgroundGradient by remember {
        mutableStateOf(
            readInitialBackgroundGradient(prefs)
        )
    }
    val dynamicColorsSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var selectedAccentColorsMode by remember {
        mutableStateOf(
            readInitialAccentColorsMode(prefs, dynamicColorsSupported)
        )
    }

    val themeOptions = listOf(
        stringResource(R.string.pref_theme_system) to Preferences.THEME_MODE_SYSTEM,
        stringResource(R.string.pref_theme_light) to Preferences.THEME_MODE_LIGHT,
        stringResource(R.string.pref_theme_dark) to Preferences.THEME_MODE_DARK
    )
    val accentColorOptions = buildAccentColorOptions(dynamicColorsSupported, context::getString)

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBackgroundGradientDialog by remember { mutableStateOf(false) }
    var showCustomGradientDialog by remember { mutableStateOf(false) }
    var customDialogStartHex by remember { mutableStateOf(customGradientStartHex) }
    var customDialogEndHex by remember { mutableStateOf(customGradientEndHex) }
    var showAccentColorsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu_settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsCategoryHeader(stringResource(R.string.pref_interface_category))

            SettingsListItem(
                title = stringResource(R.string.pref_language_title),
                summary = selectedLanguage.labelFor(languageEntries, languageValues),
                onClick = { showLanguageDialog = true }
            )

            SettingsListItem(
                title = stringResource(R.string.pref_theme_title),
                summary = selectedThemeMode.labelForPairs(themeOptions),
                onClick = { showThemeDialog = true }
            )

            SettingsListItem(
                title = stringResource(R.string.pref_background_gradient_title),
                summary = backgroundGradientLabel(selectedBackgroundGradient, backgroundOptions),
                onClick = { showBackgroundGradientDialog = true }
            )

            SettingsListItem(
                title = stringResource(R.string.pref_accent_colors_title),
                summary = accentOptionLabel(selectedAccentColorsMode, accentColorOptions),
                onClick = { showAccentColorsDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsCategoryHeader(stringResource(R.string.pref_auto_refresh_category))

            SettingsSwitchItem(
                title = stringResource(R.string.pref_auto_refresh_enabled_title),
                summary = null,
                checked = autoRefreshEnabled,
                onCheckedChange = {
                    autoRefreshEnabled = it
                    prefs.edit().putBoolean("pref_auto_refresh_enabled", it).apply()
                    AutoRefreshHelper.getInstance(context).rescheduleAlarm()
                }
            )

            SettingsListItem(
                title = stringResource(R.string.pref_auto_refresh_period_title),
                summary = autoRefreshPeriod.labelFor(periodEntries, periodValues),
                enabled = autoRefreshEnabled,
                onClick = { showPeriodDialog = true }
            )

            SettingsSwitchItem(
                title = stringResource(R.string.pref_auto_refresh_wifi_only_title),
                summary = null,
                checked = autoRefreshWifiOnly,
                enabled = autoRefreshEnabled,
                onCheckedChange = {
                    autoRefreshWifiOnly = it
                    prefs.edit().putBoolean("pref_auto_refresh_wifi_only", it).apply()
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsCategoryHeader(stringResource(R.string.pref_data_category))
            SettingsListItem(
                title = stringResource(R.string.pref_backup_restore_title),
                summary = stringResource(R.string.pref_backup_restore_summary),
                onClick = onBackupSettings
            )
        }
    }

    if (showLanguageDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_language_title),
            options = languageEntries.zip(languageValues),
            selectedValue = selectedLanguage,
            onSelect = {
                val languageChanged = selectedLanguage != it
                selectedLanguage = it
                prefs.edit().putString("pref_language", it).apply()
                if (languageChanged) {
                    SettingsRefreshScheduler.scheduleLanguageRefresh()
                }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showPeriodDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_auto_refresh_period_title),
            options = periodEntries.zip(periodValues),
            selectedValue = autoRefreshPeriod,
            onSelect = {
                autoRefreshPeriod = it
                prefs.edit().putString("pref_auto_refresh_period", it).apply()
                AutoRefreshHelper.getInstance(context).rescheduleAlarm()
            },
            onDismiss = { showPeriodDialog = false }
        )
    }

    if (showThemeDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_theme_title),
            options = themeOptions,
            selectedValue = selectedThemeMode,
            onSelect = {
                selectedThemeMode = it
                prefs.edit().putString(Preferences.KEY_PREF_THEME_MODE, it).apply()
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showBackgroundGradientDialog) {
        BackgroundGradientSelectionDialog(
            title = stringResource(R.string.pref_background_gradient_title),
            options = backgroundOptions,
            selectedValue = selectedBackgroundGradient,
            onSelect = {
                selectedBackgroundGradient = it
                prefs.edit().putString(Preferences.KEY_PREF_BACKGROUND_GRADIENT, it).apply()
                showBackgroundGradientDialog = false
            },
            onCustomClick = {
                val current = backgroundOptions.firstOrNull { it.id == selectedBackgroundGradient }
                    ?: backgroundOptions.first()
                customDialogStartHex = colorToHex(current.startColor)
                customDialogEndHex = colorToHex(current.endColor)
                showCustomGradientDialog = true
            },
            onDismiss = { showBackgroundGradientDialog = false }
        )
    }

    if (showCustomGradientDialog) {
        CustomGradientColorPickerDialog(
            title = stringResource(R.string.pref_background_gradient_custom_title),
            startColorHex = customDialogStartHex,
            endColorHex = customDialogEndHex,
            onSave = { startHex, endHex ->
                customGradientStartHex = startHex
                customGradientEndHex = endHex
                selectedBackgroundGradient = Preferences.BACKGROUND_GRADIENT_CUSTOM
                prefs.edit()
                    .putString(Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_START, startHex)
                    .putString(Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_END, endHex)
                    .putString(
                        Preferences.KEY_PREF_BACKGROUND_GRADIENT,
                        Preferences.BACKGROUND_GRADIENT_CUSTOM
                    )
                    .apply()
                showCustomGradientDialog = false
                showBackgroundGradientDialog = false
            },
            onDismiss = { showCustomGradientDialog = false }
        )
    }

    if (showAccentColorsDialog) {
        AccentColorSelectionDialog(
            title = stringResource(R.string.pref_accent_colors_title),
            options = accentColorOptions,
            selectedValue = selectedAccentColorsMode,
            onSelect = {
                selectedAccentColorsMode = it
                prefs.edit()
                    .putString(Preferences.KEY_PREF_ACCENT_COLORS_MODE, it)
                    .putBoolean(
                        Preferences.KEY_PREF_DYNAMIC_COLORS,
                        it == Preferences.ACCENT_COLORS_DYNAMIC
                    )
                    .apply()
            },
            onDismiss = { showAccentColorsDialog = false }
        )
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(options) { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedValue == value,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun String.labelFor(entries: Array<String>, values: Array<String>): String {
    val index = values.indexOf(this)
    return if (index in entries.indices) entries[index] else entries.firstOrNull() ?: this
}

private fun String.labelForPairs(options: List<Pair<String, String>>): String {
    return options.firstOrNull { it.second == this }?.first ?: this
}

@Composable
fun SettingsCategoryHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsListItem(
    title: String,
    summary: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (enabled) 1f else 0.5f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}
