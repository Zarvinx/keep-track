package com.zarvinx.keep_track

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

data class FilterMenuItem(val labelResId: Int, val filterValue: Int)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun MainTopBar(
    state: MainScreenState,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    hazeState: HazeState,
    onAddShow: () -> Unit,
    onOpenFiltersDrawer: () -> Unit
) {
    val barShape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .hazeEffect(state = hazeState, style = HazeMaterials.thin()),
            shape = barShape,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp
        ) {
            TopAppBar(
                modifier = Modifier.clip(barShape),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(onClick = onOpenFiltersDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.menu_filter_shows_list))
                    }
                },
                title = {
                    if (state.isSearching) {
                        TextField(
                            value = state.searchQuery,
                            onValueChange = state::updateSearch,
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
                                onSearch = { keyboardController?.hide() }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f),
                                unfocusedContainerColor = if (isLightTheme) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f),
                                focusedTextColor = if (isLightTheme) Color(0xFF1A1A1A) else Color.White,
                                unfocusedTextColor = if (isLightTheme) Color(0xFF1A1A1A) else Color.White,
                                focusedPlaceholderColor = if (isLightTheme) Color(0xFF1A1A1A).copy(alpha = 0.45f) else Color.White.copy(alpha = 0.50f),
                                cursorColor = if (isLightTheme) Color(0xFF1A1A1A) else Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = !state.isSearching,
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
                            IconButton(onClick = state::startSearch) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.menu_search_library)
                                )
                            }
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = state.showActionOverlay,
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
            }
        }
    }

    LaunchedEffect(state.showActionOverlay) {
        if (state.showActionOverlay) {
            state.showActionOverlay = false
        }
    }
}
