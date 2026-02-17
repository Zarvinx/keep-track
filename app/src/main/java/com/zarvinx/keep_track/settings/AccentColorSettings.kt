package com.zarvinx.keep_track.settings

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zarvinx.keep_track.Preferences
import com.zarvinx.keep_track.R
import android.graphics.Color as AndroidColor

data class AccentColorOption(
    val value: String,
    val label: String,
    val color: Color? = null
)

fun buildAccentColorOptions(
    dynamicColorsSupported: Boolean,
    stringResolver: (Int) -> String
): List<AccentColorOption> {
    val accentColorHexes = listOf(
        "Ember" to "#de6f14",
        "Gold" to "#dec314",
        "Lime" to "#97de14",
        "Forest" to "#0e8216",
        "Aqua" to "#14dea1",
        "Azure" to "#1486de",
        "Indigo" to "#2b1aad",
        "Violet" to "#861aad",
        "Crimson" to "#ad1a1a"
    )

    return buildList {
        if (dynamicColorsSupported) {
            add(
                AccentColorOption(
                    value = Preferences.ACCENT_COLORS_DYNAMIC,
                    label = stringResolver(R.string.pref_accent_colors_dynamic)
                )
            )
        }
        add(
            AccentColorOption(
                value = Preferences.ACCENT_COLORS_APP,
                label = stringResolver(R.string.pref_accent_colors_app)
            )
        )
        accentColorHexes.forEach { (name, hex) ->
            add(
                AccentColorOption(
                    value = Preferences.ACCENT_COLORS_CUSTOM_PREFIX + hex,
                    label = name,
                    color = Color(AndroidColor.parseColor(hex))
                )
            )
        }
    }
}

fun readInitialAccentColorsMode(
    prefs: SharedPreferences,
    dynamicColorsSupported: Boolean
): String {
    val mode = if (prefs.contains(Preferences.KEY_PREF_ACCENT_COLORS_MODE)) {
        prefs.getString(
            Preferences.KEY_PREF_ACCENT_COLORS_MODE,
            Preferences.ACCENT_COLORS_DYNAMIC
        ) ?: Preferences.ACCENT_COLORS_DYNAMIC
    } else {
        val legacyDynamicEnabled = prefs.getBoolean(Preferences.KEY_PREF_DYNAMIC_COLORS, true)
        if (legacyDynamicEnabled) Preferences.ACCENT_COLORS_DYNAMIC else Preferences.ACCENT_COLORS_APP
    }

    return if (!dynamicColorsSupported && mode == Preferences.ACCENT_COLORS_DYNAMIC) {
        Preferences.ACCENT_COLORS_APP
    } else if (mode == Preferences.ACCENT_COLORS_APP ||
        mode == Preferences.ACCENT_COLORS_DYNAMIC ||
        mode.startsWith(Preferences.ACCENT_COLORS_CUSTOM_PREFIX)
    ) {
        mode
    } else {
        Preferences.ACCENT_COLORS_APP
    }
}

fun accentOptionLabel(value: String, options: List<AccentColorOption>): String {
    return options.firstOrNull { it.value == value }?.label ?: value
}

@Composable
fun AccentColorSelectionDialog(
    title: String,
    options: List<AccentColorOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(options) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option.value)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedValue == option.value,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        if (option.color == null) {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(24.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(
                                        color = option.color,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
