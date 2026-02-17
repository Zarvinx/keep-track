/*
 * Copyright (C) 2026 Zarvinx
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zarvinx.keep_track.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * A text component that automatically scrolls horizontally when the text is too long to fit.
 * The text pauses briefly at the start before scrolling.
 *
 * @param text The text to display
 * @param modifier Modifier to be applied to the text
 * @param color Text color
 * @param fontSize Font size
 * @param style Text style
 * @param maxLines Maximum number of lines (default 1 for marquee effect)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    shadow: Shadow? = null,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            initialDelayMillis = 2000,
            spacing = MarqueeSpacing(50.dp),
            velocity = 40.dp
        ),
        color = color,
        fontSize = fontSize,
        style = if (shadow != null) style.copy(shadow = shadow) else style,
        maxLines = maxLines,
        softWrap = false
    )
}
