package com.redcoracle.episodes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.ui.theme.EpisodesTheme

class BackupSettingsActivity : AppCompatActivity() {
    private var showBackupDialog by mutableStateOf(false)
    private lateinit var backupRestoreCoordinator: BackupRestoreCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        backupRestoreCoordinator = BackupRestoreCoordinator(
            activity = this,
            showLegacyRestoreDialog = { showBackupDialog = true }
        )

        setContent {
            EpisodesTheme {
                BackupSettingsScreen(
                    onNavigateBack = { finish() },
                    onBackupNow = backupRestoreCoordinator::backUp,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onNavigateBack: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val frequencyLabels = remember {
        mapOf(
            AutoBackupHelper.PERIOD_DAILY to context.getString(R.string.pref_auto_backup_frequency_daily),
            AutoBackupHelper.PERIOD_WEEKLY to context.getString(R.string.pref_auto_backup_frequency_weekly),
            AutoBackupHelper.PERIOD_MONTHLY to context.getString(R.string.pref_auto_backup_frequency_monthly)
        )
    }

    var autoBackupEnabled by remember {
        mutableStateOf(prefs.getBoolean(AutoBackupHelper.KEY_PREF_AUTO_BACKUP_ENABLED, false))
    }
    var autoBackupFrequency by remember {
        mutableStateOf(
            prefs.getString(
                AutoBackupHelper.KEY_PREF_AUTO_BACKUP_PERIOD,
                AutoBackupHelper.PERIOD_WEEKLY
            ) ?: AutoBackupHelper.PERIOD_WEEKLY
        )
    }
    var backupRetention by remember {
        mutableStateOf(prefs.getInt(AutoBackupHelper.KEY_PREF_AUTO_BACKUP_RETENTION, 10))
    }

    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pref_backup_restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
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
            SettingsCategoryHeader(stringResource(R.string.pref_backup_manual_category))
            SettingsListItem(
                title = stringResource(R.string.pref_backup_now_title),
                summary = stringResource(R.string.pref_backup_now_summary),
                onClick = onBackupNow
            )
            SettingsListItem(
                title = stringResource(R.string.menu_restore),
                summary = stringResource(R.string.pref_restore_summary),
                onClick = onRestore
            )

            SettingsCategoryHeader(stringResource(R.string.pref_backup_auto_category))
            SettingsSwitchItem(
                title = stringResource(R.string.pref_auto_backup_enabled_title),
                summary = stringResource(R.string.pref_auto_backup_enabled_summary),
                checked = autoBackupEnabled,
                onCheckedChange = {
                    autoBackupEnabled = it
                    prefs.edit().putBoolean(AutoBackupHelper.KEY_PREF_AUTO_BACKUP_ENABLED, it).apply()
                    AutoBackupHelper.getInstance(context).rescheduleAlarm()
                }
            )
            SettingsListItem(
                title = stringResource(R.string.pref_auto_backup_frequency_title),
                summary = frequencyLabels[autoBackupFrequency]
                    ?: frequencyLabels[AutoBackupHelper.PERIOD_WEEKLY].orEmpty(),
                enabled = autoBackupEnabled,
                onClick = { showFrequencyDialog = true }
            )
            SettingsListItem(
                title = stringResource(R.string.pref_auto_backup_retention_title),
                summary = context.getString(R.string.pref_auto_backup_retention_summary, backupRetention),
                onClick = { showRetentionDialog = true }
            )
            SettingsListItem(
                title = stringResource(R.string.pref_backup_location_title),
                summary = FileUtilities.get_backup_directory(context).absolutePath,
                enabled = false,
                onClick = {}
            )
        }
    }

    if (showFrequencyDialog) {
        SelectionDialog(
            title = stringResource(R.string.pref_auto_backup_frequency_title),
            options = listOf(
                stringResource(R.string.pref_auto_backup_frequency_daily) to AutoBackupHelper.PERIOD_DAILY,
                stringResource(R.string.pref_auto_backup_frequency_weekly) to AutoBackupHelper.PERIOD_WEEKLY,
                stringResource(R.string.pref_auto_backup_frequency_monthly) to AutoBackupHelper.PERIOD_MONTHLY
            ),
            selectedValue = autoBackupFrequency,
            onSelect = {
                autoBackupFrequency = it
                prefs.edit().putString(AutoBackupHelper.KEY_PREF_AUTO_BACKUP_PERIOD, it).apply()
                AutoBackupHelper.getInstance(context).rescheduleAlarm()
            },
            onDismiss = { showFrequencyDialog = false }
        )
    }

    if (showRetentionDialog) {
        var retentionInput by remember(showRetentionDialog) { mutableStateOf(backupRetention.toString()) }
        AlertDialog(
            onDismissRequest = { showRetentionDialog = false },
            title = { Text(stringResource(R.string.pref_auto_backup_retention_title)) },
            text = {
                OutlinedTextField(
                    value = retentionInput,
                    onValueChange = { input ->
                        retentionInput = input.filter(Char::isDigit).take(3)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.pref_auto_backup_retention_hint)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val retention = retentionInput.toIntOrNull()?.coerceIn(1, 100) ?: backupRetention
                        backupRetention = retention
                        prefs.edit().putInt(AutoBackupHelper.KEY_PREF_AUTO_BACKUP_RETENTION, retention).apply()
                        AutoBackupHelper.getInstance(context).pruneBackupsNow()
                        showRetentionDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRetentionDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
