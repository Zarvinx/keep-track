package com.redcoracle.keep_track.settings

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.redcoracle.keep_track.Preferences
import com.redcoracle.keep_track.ui.theme.BackgroundGradientOption
import com.redcoracle.keep_track.ui.theme.backgroundGradientOptions
import com.redcoracle.keep_track.ui.theme.defaultCustomBackgroundEnd
import com.redcoracle.keep_track.ui.theme.defaultCustomBackgroundStart
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController

private const val COLOR_MAX = 255f

fun readInitialCustomBackgroundStartHex(prefs: SharedPreferences): String {
    return prefs.getString(
        Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_START,
        colorToHex(defaultCustomBackgroundStart())
    ) ?: colorToHex(defaultCustomBackgroundStart())
}

fun readInitialCustomBackgroundEndHex(prefs: SharedPreferences): String {
    return prefs.getString(
        Preferences.KEY_PREF_BACKGROUND_GRADIENT_CUSTOM_END,
        colorToHex(defaultCustomBackgroundEnd())
    ) ?: colorToHex(defaultCustomBackgroundEnd())
}

fun parseColorHexOrDefault(colorHex: String, defaultColor: Color): Color {
    val normalized = colorHex.removePrefix("#")
    if (normalized.length != 6) return defaultColor
    return try {
        val intValue = normalized.toLong(16).toInt()
        Color(
            red = ((intValue shr 16) and 0xFF) / COLOR_MAX,
            green = ((intValue shr 8) and 0xFF) / COLOR_MAX,
            blue = (intValue and 0xFF) / COLOR_MAX
        )
    } catch (_: NumberFormatException) {
        defaultColor
    }
}

fun colorToHex(color: Color): String {
    val red = (color.red * COLOR_MAX).toInt().coerceIn(0, 255)
    val green = (color.green * COLOR_MAX).toInt().coerceIn(0, 255)
    val blue = (color.blue * COLOR_MAX).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(red, green, blue)
}

fun buildBackgroundGradientOptions(
    customStartHex: String,
    customEndHex: String
): List<BackgroundGradientOption> {
    val start = parseColorHexOrDefault(customStartHex, defaultCustomBackgroundStart())
    val end = parseColorHexOrDefault(customEndHex, defaultCustomBackgroundEnd())
    return backgroundGradientOptions(start, end)
}

fun readInitialBackgroundGradient(
    prefs: SharedPreferences
): String {
    val saved = prefs.getString(
        Preferences.KEY_PREF_BACKGROUND_GRADIENT,
        Preferences.BACKGROUND_GRADIENT_MIST_BLUE
    ) ?: Preferences.BACKGROUND_GRADIENT_MIST_BLUE

    val validIds = backgroundGradientOptions(
        defaultCustomBackgroundStart(),
        defaultCustomBackgroundEnd()
    ).map { it.id }.toSet()
    return if (saved in validIds) saved else Preferences.BACKGROUND_GRADIENT_MIST_BLUE
}

fun backgroundGradientLabel(
    value: String,
    options: List<BackgroundGradientOption>
): String {
    return options.firstOrNull { it.id == value }?.label ?: value
}

@Composable
fun BackgroundGradientSelectionDialog(
    title: String,
    options: List<BackgroundGradientOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onCustomClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        rowOptions.forEach { option ->
                            val isSelected = selectedValue == option.id
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        },
                                        shape = CircleShape
                                    )
                                    .background(
                                        brush = Brush.linearGradient(
                                            listOf(option.startColor, option.endColor)
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (option.id == Preferences.BACKGROUND_GRADIENT_CUSTOM) {
                                            onCustomClick()
                                        } else {
                                            onSelect(option.id)
                                        }
                                    }
                            ) {
                                if (option.id == Preferences.BACKGROUND_GRADIENT_CUSTOM) {
                                    Box(
                                        modifier = Modifier
                                            .align(androidx.compose.ui.Alignment.Center)
                                            .size(22.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                                shape = CircleShape
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Build,
                                            contentDescription = "Custom gradient",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .align(androidx.compose.ui.Alignment.Center)
                                                .size(14.dp)
                                        )
                                    }
                                }
                            }
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

@Composable
fun CustomGradientColorPickerDialog(
    title: String,
    startColorHex: String,
    endColorHex: String,
    onSave: (startHex: String, endHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    var startColor by remember(startColorHex) {
        mutableStateOf(parseColorHexOrDefault(startColorHex, defaultCustomBackgroundStart()))
    }
    var endColor by remember(endColorHex) {
        mutableStateOf(parseColorHexOrDefault(endColorHex, defaultCustomBackgroundEnd()))
    }
    val startController = rememberColorPickerController()
    val endController = rememberColorPickerController()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Start color", style = MaterialTheme.typography.bodyMedium)
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    controller = startController,
                    initialColor = startColor,
                    onColorChanged = { envelope ->
                        if (envelope.fromUser) {
                            startColor = envelope.color
                        }
                    }
                )
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    controller = startController
                )
                Text(
                    text = colorToHex(startColor),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("End color", style = MaterialTheme.typography.bodyMedium)
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    controller = endController,
                    initialColor = endColor,
                    onColorChanged = { envelope ->
                        if (envelope.fromUser) {
                            endColor = envelope.color
                        }
                    }
                )
                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    controller = endController
                )
                Text(
                    text = colorToHex(endColor),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .background(
                            brush = Brush.linearGradient(listOf(startColor, endColor)),
                            shape = CircleShape
                        )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(colorToHex(startColor), colorToHex(endColor)) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
