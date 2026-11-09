package com.zarvinx.keep_track

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

data class FilterMenuItem(val labelResId: Int, val filterValue: Int)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainTopBar(
    state: MainScreenState,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surface, shadowElevation = 4.dp) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant.copy(alpha = 0.75f)
        ) {
            Row(
                modifier = Modifier.height(48.dp).padding(start = 14.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = colors.onSurfaceVariant)
                BasicTextField(
                    value = state.searchQuery,
                    onValueChange = state::updateSearch,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    decorationBox = { innerTextField ->
                        if (state.searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.menu_search_library_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = colors.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { state.updateSearch("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.menu_close_search))
                    }
                }
            }
        }
    }
}
