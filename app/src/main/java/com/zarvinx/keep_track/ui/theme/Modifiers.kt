package com.zarvinx.keep_track.ui.theme

import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

fun Modifier.drawerSelectionHighlight(
    selected: Boolean,
    accentColor: Color,
    isLightTheme: Boolean,
    cornerRadius: Dp = 10.dp
): Modifier = drawBehind {
    if (!selected) return@drawBehind

    val radius = CornerRadius(cornerRadius.toPx())
    drawRoundRect(
        color = accentColor.copy(alpha = if (isLightTheme) 0.08f else 0.12f),
        cornerRadius = radius
    )

    val indicatorWidth = 2.dp.toPx()
    val indicatorInset = 10.dp.toPx()
    drawRoundRect(
        color = accentColor.copy(alpha = if (isLightTheme) 0.85f else 0.75f),
        topLeft = Offset(7.dp.toPx(), indicatorInset),
        size = Size(indicatorWidth, size.height - indicatorInset * 2),
        cornerRadius = CornerRadius(indicatorWidth / 2)
    )
}
