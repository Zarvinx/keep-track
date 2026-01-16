package com.redcoracle.episodes.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow

/**
 * Common shadow styles used throughout the app
 */
object AppShadows {
    /**
     * Text shadow for overlays on images to improve readability
     */
    val TextOnImage = Shadow(
        color = Color.Black.copy(alpha = 0.5f),
        offset = Offset(2f, 2f),
        blurRadius = 4f
    )
}
