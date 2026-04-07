package com.zarvinx.keep_track.ui.theme

import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun drawerItemColors(contentColor: Color) = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = Color.Transparent,
    unselectedContainerColor = Color.Transparent,
    selectedTextColor = contentColor,
    unselectedTextColor = contentColor.copy(alpha = 0.72f)
)

@Composable
fun drawerActionItemColors(contentColor: Color) = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = contentColor.copy(alpha = 0.12f),
    unselectedContainerColor = Color.Transparent,
    selectedTextColor = contentColor,
    unselectedTextColor = contentColor.copy(alpha = 0.72f)
)

fun Modifier.sunkenPill(
    selected: Boolean,
    accentColor: Color,
    cornerRadius: Dp = 10.dp
): Modifier = drawBehind {
    if (!selected) return@drawBehind

    val r = cornerRadius.toPx()
    val cr = CornerRadius(r)
    val path = Path().apply {
        addRoundRect(RoundRect(0f, 0f, size.width, size.height, cr, cr, cr, cr))
    }

    // Dark fill
    drawPath(path, Color.Black.copy(alpha = 0.45f))

    // Top inner shadow
    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
            startY = 0f,
            endY = 10.dp.toPx()
        )
    )

    // Bottom inner highlight
    drawPath(
        path,
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.06f)),
            startY = size.height - 8.dp.toPx(),
            endY = size.height
        )
    )

    // Accent inner glow — fades to transparent before center, no sharp cutoff
    val glowAlpha = 0.25f
    val glowSize = 10.dp.toPx()
    clipPath(path) {
        drawRect(brush = Brush.verticalGradient(
            colors = listOf(accentColor.copy(alpha = glowAlpha), Color.Transparent),
            startY = 0f, endY = glowSize
        ))
        drawRect(brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, accentColor.copy(alpha = glowAlpha)),
            startY = size.height - glowSize, endY = size.height
        ))
        drawRect(brush = Brush.horizontalGradient(
            colors = listOf(accentColor.copy(alpha = glowAlpha), Color.Transparent),
            startX = 0f, endX = glowSize
        ))
        drawRect(brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, accentColor.copy(alpha = glowAlpha)),
            startX = size.width - glowSize, endX = size.width
        ))
    }

    // Accent rim line
    drawPath(path, color = accentColor.copy(alpha = 0.45f), style = Stroke(width = 1.2.dp.toPx()))
}
