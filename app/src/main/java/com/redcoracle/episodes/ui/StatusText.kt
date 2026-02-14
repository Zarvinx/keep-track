package com.redcoracle.episodes.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Beenhere
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val endedStatuses = setOf("Ended", "Canceled")

private data class ShowStatusMessage(
    val text: String,
    val isEnded: Boolean
)

@Composable
fun StatusText(
    status: String?,
    modifier: Modifier = Modifier
) {
    val statusMessage = getShowStatusMessage(status)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (statusMessage.isEnded) {
            Icon(
                imageVector = Icons.Filled.Beenhere,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = statusMessage.text,
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun getShowStatusMessage(status: String?): ShowStatusMessage {
    val isEnded = status in endedStatuses
    return ShowStatusMessage(
        text = if (isEnded) "This show has ended" else "You are all caught up",
        isEnded = isEnded
    )
}
