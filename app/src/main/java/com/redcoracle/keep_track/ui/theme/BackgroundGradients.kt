package com.redcoracle.keep_track.ui.theme

import androidx.compose.ui.graphics.Color
import com.redcoracle.keep_track.Preferences

data class BackgroundGradientOption(
    val id: String,
    val label: String,
    val startColor: Color,
    val endColor: Color
)

private val defaultCustomStart = Color(0xFFF5F5F5)
private val defaultCustomEnd = Color(0xFF2A456F)

fun defaultCustomBackgroundStart(): Color = defaultCustomStart

fun defaultCustomBackgroundEnd(): Color = defaultCustomEnd

fun backgroundGradientOptions(
    customStartColor: Color = defaultCustomStart,
    customEndColor: Color = defaultCustomEnd
): List<BackgroundGradientOption> = listOf(
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_MIST_BLUE,
        label = "Mist Blue",
        startColor = Color(0xFFF5F5F5),
        endColor = Color(0xFF2A456F)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_SUNSET_AMBER,
        label = "Sunset Amber",
        startColor = Color(0xFFFBEFE4),
        endColor = Color(0xFFDE6F14)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_CITRUS_LIME,
        label = "Citrus Lime",
        startColor = Color(0xFFF7FBE6),
        endColor = Color(0xFF97DE14)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_DEEP_FOREST,
        label = "Deep Forest",
        startColor = Color(0xFFEAF6EC),
        endColor = Color(0xFF0E8216)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_AQUA_SKY,
        label = "Aqua Sky",
        startColor = Color(0xFFE6FAF5),
        endColor = Color(0xFF1486DE)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_ROYAL_PLUM,
        label = "Royal Plum",
        startColor = Color(0xFFF0ECFA),
        endColor = Color(0xFF861AAD)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_NIGHT_BLUE,
        label = "Night Blue",
        startColor = Color(0xFF3154B5),
        endColor = Color(0xFF0E1733)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_DEEP_OCEAN,
        label = "Deep Ocean",
        startColor = Color(0xFF2A63E6),
        endColor = Color(0xFF0A132C)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_NIGHT_MINT,
        label = "Night Mint",
        startColor = Color(0xFF1A8A81),
        endColor = Color(0xFF0B2A25)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_PINE_SHADOW,
        label = "Pine Shadow",
        startColor = Color(0xFF1D7A40),
        endColor = Color(0xFF0A1D16)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_BURNT_AMBER,
        label = "Burnt Amber",
        startColor = Color(0xFFC76211),
        endColor = Color(0xFF1B1613)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_VIOLET_NIGHT,
        label = "Violet Night",
        startColor = Color(0xFF9333EA),
        endColor = Color(0xFF1B1830)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_CRIMSON_NIGHT,
        label = "Crimson Night",
        startColor = Color(0xFFCC2A2A),
        endColor = Color(0xFF2A1013)
    ),
    BackgroundGradientOption(
        id = Preferences.BACKGROUND_GRADIENT_CUSTOM,
        label = "Custom",
        startColor = customStartColor,
        endColor = customEndColor
    )
)

fun findBackgroundGradientOption(
    id: String,
    customStartColor: Color = defaultCustomStart,
    customEndColor: Color = defaultCustomEnd
): BackgroundGradientOption {
    val options = backgroundGradientOptions(customStartColor, customEndColor)
    return options.firstOrNull { it.id == id } ?: options.first()
}
