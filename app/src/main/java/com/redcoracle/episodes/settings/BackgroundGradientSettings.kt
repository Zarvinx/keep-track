package com.redcoracle.episodes.settings

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.redcoracle.episodes.Preferences
import com.redcoracle.episodes.ui.theme.BackgroundGradientOption
import com.redcoracle.episodes.ui.theme.backgroundGradientOptions

fun readInitialBackgroundGradient(
    prefs: SharedPreferences
): String {
    val saved = prefs.getString(
        Preferences.KEY_PREF_BACKGROUND_GRADIENT,
        Preferences.BACKGROUND_GRADIENT_MIST_BLUE
    ) ?: Preferences.BACKGROUND_GRADIENT_MIST_BLUE

    val validIds = backgroundGradientOptions().map { it.id }.toSet()
    return if (saved in validIds) saved else Preferences.BACKGROUND_GRADIENT_MIST_BLUE
}

@Composable
fun BackgroundGradientSelectionDialog(
    title: String,
    options: List<BackgroundGradientOption>,
    selectedValue: String,
    onSelect: (String) -> Unit,
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
                                    .clickable { onSelect(option.id) }
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
