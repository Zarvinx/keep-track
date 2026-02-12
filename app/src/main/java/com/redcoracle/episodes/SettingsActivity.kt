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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.ui.theme.EpisodesTheme

class SettingsActivity : AppCompatActivity() {
    private var showBackupDialog by mutableStateOf(false)
    private lateinit var backupRestoreCoordinator: BackupRestoreCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backupRestoreCoordinator = BackupRestoreCoordinator(
            activity = this,
            hasStoragePermission = this::hasStoragePermission,
            showLegacyRestoreDialog = { showBackupDialog = true }
        )

        setContent {
            EpisodesTheme {
                SettingsScreen(
                    onNavigateBack = { finish() },
                    onBackup = backupRestoreCoordinator::backUp,
                    onRestore = backupRestoreCoordinator::restore
                )

                if (showBackupDialog) {
                    SelectBackupDialog(
                        onBackupSelected = { backupFilename ->
                            showBackupDialog = false
                            backupRestoreCoordinator.onBackupSelected(backupFilename)
                        },
                        onDismiss = { showBackupDialog = false }
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (backupRestoreCoordinator.onActivityResult(requestCode, resultCode, data)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun hasStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    
    val languageEntries = context.resources.getStringArray(R.array.language_entries)
    val languageValues = context.resources.getStringArray(R.array.language_values)
    val periodEntries = context.resources.getStringArray(R.array.auto_refresh_period_entries)
    val periodValues = context.resources.getStringArray(R.array.auto_refresh_period_values)
    
    var selectedLanguage by remember { mutableStateOf(prefs.getString("pref_language", "en") ?: "en") }
    var reverseSortOrder by remember { mutableStateOf(prefs.getBoolean("reverse_sort_order", false)) }
    var autoRefreshEnabled by remember { mutableStateOf(prefs.getBoolean("pref_auto_refresh_enabled", false)) }
    var autoRefreshPeriod by remember { mutableStateOf(prefs.getString("pref_auto_refresh_period", "168") ?: "168") }
    var autoRefreshWifiOnly by remember { mutableStateOf(prefs.getBoolean("pref_auto_refresh_wifi_only", true)) }
    
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    
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
                summary = languageEntries[languageValues.indexOf(selectedLanguage)],
                onClick = { showLanguageDialog = true }
            )
            
            SettingsSwitchItem(
                title = stringResource(R.string.pref_reverse_sort_order),
                summary = stringResource(R.string.pref_reverse_sort_order_summary),
                checked = reverseSortOrder,
                onCheckedChange = { 
                    reverseSortOrder = it
                    prefs.edit().putBoolean("reverse_sort_order", it).apply()
                }
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
                summary = periodEntries[periodValues.indexOf(autoRefreshPeriod)],
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
                onClick = { showBackupRestoreDialog = true }
            )
        }
    }
    
    if (showLanguageDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_language_title),
            options = languageEntries.zip(languageValues),
            selectedValue = selectedLanguage,
            onSelect = { 
                selectedLanguage = it
                prefs.edit().putString("pref_language", it).apply()
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

    if (showBackupRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showBackupRestoreDialog = false },
            title = { Text(stringResource(R.string.pref_backup_restore_title)) },
            text = { Text(stringResource(R.string.pref_backup_restore_summary)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupRestoreDialog = false
                        onBackup()
                    }
                ) {
                    Text(stringResource(R.string.menu_back_up))
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showBackupRestoreDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            showBackupRestoreDialog = false
                            onRestore()
                        }
                    ) {
                        Text(stringResource(R.string.menu_restore))
                    }
                }
            }
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
            Column {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
