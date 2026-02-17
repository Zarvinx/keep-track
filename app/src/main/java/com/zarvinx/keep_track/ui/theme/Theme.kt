package com.zarvinx.keep_track.ui.theme

import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import com.zarvinx.keep_track.Preferences

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFFFB74D)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF388E3C),
    tertiary = Color(0xFFF57C00)
)

@Composable
fun KeepTrackTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeMode = rememberThemeMode()
    val accentColorsMode = rememberAccentColorsMode()
    val darkTheme = when (themeMode) {
        Preferences.THEME_MODE_LIGHT -> false
        Preferences.THEME_MODE_DARK -> true
        else -> isSystemInDarkTheme()
    }
    val useDynamicColors = accentColorsMode == Preferences.ACCENT_COLORS_DYNAMIC

    val baseScheme = when {
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = applyCustomAccentIfSelected(baseScheme, accentColorsMode)

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
private fun rememberThemeMode(): String {
    val context = LocalContext.current
    val prefs = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    var themeMode by remember {
        mutableStateOf(
            prefs.getString(Preferences.KEY_PREF_THEME_MODE, Preferences.THEME_MODE_SYSTEM)
                ?: Preferences.THEME_MODE_SYSTEM
        )
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == Preferences.KEY_PREF_THEME_MODE) {
                themeMode = sharedPrefs.getString(
                    Preferences.KEY_PREF_THEME_MODE,
                    Preferences.THEME_MODE_SYSTEM
                ) ?: Preferences.THEME_MODE_SYSTEM
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return themeMode
}

@Composable
private fun rememberAccentColorsMode(): String {
    val context = LocalContext.current
    val prefs = remember(context) { PreferenceManager.getDefaultSharedPreferences(context) }
    var accentMode by remember {
        mutableStateOf(readAccentColorsMode(prefs))
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == Preferences.KEY_PREF_ACCENT_COLORS_MODE ||
                key == Preferences.KEY_PREF_DYNAMIC_COLORS
            ) {
                accentMode = readAccentColorsMode(sharedPrefs)
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    return accentMode
}

private fun readAccentColorsMode(prefs: SharedPreferences): String {
    if (prefs.contains(Preferences.KEY_PREF_ACCENT_COLORS_MODE)) {
        return prefs.getString(
            Preferences.KEY_PREF_ACCENT_COLORS_MODE,
            Preferences.ACCENT_COLORS_DYNAMIC
        ) ?: Preferences.ACCENT_COLORS_DYNAMIC
    }

    val legacyDynamicEnabled = prefs.getBoolean(Preferences.KEY_PREF_DYNAMIC_COLORS, true)
    return if (legacyDynamicEnabled) {
        Preferences.ACCENT_COLORS_DYNAMIC
    } else {
        Preferences.ACCENT_COLORS_APP
    }
}

private fun applyCustomAccentIfSelected(
    baseScheme: ColorScheme,
    accentMode: String
): ColorScheme {
    val customAccent = parseCustomAccentColor(accentMode) ?: return baseScheme
    val onAccent = if (customAccent.luminance() > 0.5f) {
        Color(0xFF1A1A1A)
    } else {
        Color(0xFFF2F2F2)
    }
    return baseScheme.copy(
        primary = customAccent,
        onPrimary = onAccent
    )
}

private fun parseCustomAccentColor(accentMode: String): Color? {
    if (!accentMode.startsWith(Preferences.ACCENT_COLORS_CUSTOM_PREFIX)) return null
    val colorHex = accentMode.removePrefix(Preferences.ACCENT_COLORS_CUSTOM_PREFIX)
    return try {
        Color(AndroidColor.parseColor(colorHex))
    } catch (_: IllegalArgumentException) {
        null
    }
}
