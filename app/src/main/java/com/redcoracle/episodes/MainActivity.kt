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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.ui.ShowsListScreen
import com.redcoracle.episodes.ui.ShowsViewModel
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.redcoracle.episodes.db.DatabaseOpenHelper
import com.redcoracle.episodes.db.ShowsProvider
import com.redcoracle.episodes.services.AsyncTask
import com.redcoracle.episodes.services.BackupTask
import com.redcoracle.episodes.services.RefreshAllShowsTask
import com.redcoracle.episodes.services.RestoreTask
import com.redcoracle.episodes.ui.theme.EpisodesTheme
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), 
    ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        private var context: Context? = null
        private const val WRITE_REQUEST_CODE = 0
        private const val READ_REQUEST_CODE = 1

        @JvmStatic
        fun getAppContext(): Context? = context
    }
    
    private var showBackupDialog by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        context = applicationContext
        AutoRefreshHelper.getInstance(applicationContext).rescheduleAlarm()
        
        // Hide the default action bar to prevent double title bars
        supportActionBar?.hide()

        setContent {
            EpisodesTheme {
                val viewModel: ShowsViewModel = viewModel()
                
                MainScreen(
                    viewModel = viewModel,
                    onShowSelected = { showId -> onShowSelected(showId) },
                    onBackup = { backUp() },
                    onRestore = { restore() },
                    onSettings = { showSettings() },
                    onAbout = { showAbout() },
                    onRefreshAll = { refreshAllShows() },
                    onAddShow = {
                        val intent = Intent(this, AddShowSearchActivity::class.java)
                        startActivity(intent)
                    },
                    showBackupDialog = showBackupDialog,
                    onDismissBackupDialog = { showBackupDialog = false },
                    onBackupSelected = { backupFilename -> onBackupSelected(backupFilename) }
                )
            }
        }
    }

    fun onShowSelected(showId: Int) {
        val intent = Intent(this, ShowActivity::class.java)
        intent.putExtra("showId", showId)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun hasStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun backUp() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/x-sqlite3"
                putExtra(Intent.EXTRA_TITLE, FileUtilities.get_suggested_filename())
            }
            startActivityForResult(intent, WRITE_REQUEST_CODE)
        } else {
            if (hasStoragePermission()) {
                AsyncTask().executeAsync(BackupTask(FileUtilities.get_suggested_filename()))
            }
        }
    }

    private fun restore() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/x-sqlite3"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/octet-stream"))
            }
            startActivityForResult(intent, READ_REQUEST_CODE)
        } else {
            if (hasStoragePermission()) {
                showBackupDialog = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                uri?.let {
                    FileUtilities.copy_file(
                        FileInputStream(getDatabasePath(DatabaseOpenHelper.getDbName())).channel,
                        FileOutputStream(contentResolver.openFileDescriptor(it, "w")?.fileDescriptor).channel
                    )
                    Toast.makeText(
                        this,
                        String.format(
                            getString(R.string.back_up_success_message),
                            FileUtilities.uri_to_filename(this, it)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                uri?.let {
                    FileUtilities.copy_file(
                        FileInputStream(contentResolver.openFileDescriptor(it, "r")?.fileDescriptor).channel,
                        FileOutputStream(getDatabasePath(DatabaseOpenHelper.getDbName())).channel
                    )
                    ShowsProvider.reloadDatabase(this)
                    CoroutineScope(Dispatchers.IO).launch {
                        Glide.get(applicationContext).clearDiskCache()
                    }
                    Toast.makeText(this, getString(R.string.restore_success_message), Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun onBackupSelected(backupFilename: String) {
        showBackupDialog = false
        AsyncTask().executeAsync(RestoreTask(backupFilename))
    }
    
    private fun refreshAllShows() {
        AsyncTask().executeAsync(RefreshAllShowsTask())
        Toast.makeText(this, "Refreshing all shows in background...", Toast.LENGTH_LONG).show()
    }

    private fun showSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun showAbout() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }
}

// Menu item data classes (outside Composable to avoid recreation on recompose)
private data class FilterMenuItem(val labelResId: Int, val filterValue: Int)
private data class MainMenuItem(val labelResId: Int, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    viewModel: ShowsViewModel,
    onShowSelected: (Int) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    onRefreshAll: () -> Unit,
    onAddShow: () -> Unit,
    showBackupDialog: Boolean,
    onDismissBackupDialog: () -> Unit,
    onBackupSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var showActionOverlay by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Filter menu items (using constants from ShowsViewModel)
    val filterMenuItems = remember {
        listOf(
            FilterMenuItem(R.string.menu_filter_uncompleted, ShowsViewModel.SHOWS_FILTER_WATCHING),
            FilterMenuItem(R.string.menu_filter_starred, ShowsViewModel.SHOWS_FILTER_STARRED),
            FilterMenuItem(R.string.menu_filter_archived, ShowsViewModel.SHOWS_FILTER_ARCHIVED),
            FilterMenuItem(R.string.menu_filter_upcoming, ShowsViewModel.SHOWS_FILTER_UPCOMING),
            FilterMenuItem(R.string.menu_filter_all, ShowsViewModel.SHOWS_FILTER_ALL)
        )
    }
    
    // Main menu items
    val mainMenuItems = remember(onBackup, onRestore, onRefreshAll, onSettings, onAbout) {
        listOf(
            MainMenuItem(R.string.menu_back_up, onBackup),
            MainMenuItem(R.string.menu_refresh_all_shows, onRefreshAll),
            MainMenuItem(R.string.menu_restore, onRestore),
            MainMenuItem(R.string.menu_settings, onSettings),
            MainMenuItem(R.string.menu_about, onAbout)
        )
    }
    
    fun applyFilter(filter: Int) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putInt(ShowsViewModel.KEY_PREF_SHOWS_FILTER, filter).apply()
    }

    fun updateSearch(query: String) {
        searchQuery = query
        viewModel.setSearchQuery(query)
    }

    fun closeSearch() {
        isSearching = false
        updateSearch("")
        keyboardController?.hide()
    }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    BackHandler(enabled = isSearching) {
        closeSearch()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                closeSearch()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            Box {
                TopAppBar(
                    title = { 
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { updateSearch(it) },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.menu_search_library_hint),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.focusRequester(focusRequester),
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        } else {
                            Text(stringResource(R.string.app_name))
                        }
                    },
                    actions = {
                        AnimatedVisibility(
                            visible = !isSearching,
                            enter = slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(260)
                            ) + fadeIn(animationSpec = tween(200)),
                            exit = ExitTransition.None
                        ) {
                            Row {
                                IconButton(onClick = onAddShow) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.menu_add_new_show))
                                }
                                IconButton(onClick = {
                                    showActionOverlay = true
                                    isSearching = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.menu_search_library)
                                    )
                                }
                                IconButton(onClick = { showFilterMenu = true }) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_menu_filter_shows_list),
                                        contentDescription = stringResource(R.string.menu_filter_shows_list)
                                    )
                                }
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            filterMenuItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(item.labelResId)) },
                                    onClick = {
                                        showFilterMenu = false
                                        applyFilter(item.filterValue)
                                    }
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            mainMenuItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(item.labelResId)) },
                                    onClick = {
                                        showMenu = false
                                        item.action()
                                    }
                                )
                            }
                        }
                    }
                )
                AnimatedVisibility(
                    visible = showActionOverlay,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    enter = EnterTransition.None,
                    exit = slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(260)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    Row {
                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                        IconButton(onClick = {}, enabled = false) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_menu_filter_shows_list),
                                contentDescription = null
                            )
                        }
                        IconButton(onClick = {}, enabled = false) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                    }
                }
            }
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
    
    // Backup selection dialog
    if (showBackupDialog) {
        SelectBackupDialog(
            onBackupSelected = onBackupSelected,
            onDismiss = onDismissBackupDialog
        )
    }

    LaunchedEffect(showActionOverlay) {
        if (showActionOverlay) {
            showActionOverlay = false
        }
    }
}
