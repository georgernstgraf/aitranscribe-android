package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.ui.theme.Red

@Composable
fun BottomControlPanel(
    isRecording: Boolean,
    recordingDuration: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    currentFilter: ViewFilter,
    onFilterChanged: (ViewFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121C26))
            .border(1.dp, Color(0xFF243241), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter pills take up available space
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ViewFilter.entries.forEach { filter ->
                val isSelected = currentFilter == filter
                FilterPill(
                    label = filter.label,
                    selected = isSelected,
                    onClick = { onFilterChanged(filter) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Record button
        RecordButton(
            isRecording = isRecording,
            recordingDuration = recordingDuration,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording
        )
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    recordingDuration: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isRecording) Red else Color(0xFF69C3F0)
    val icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable {
                if (isRecording) onStopRecording() else onStartRecording()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Stop Recording",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = formatDuration(recordingDuration),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = "Start Recording",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) Color(0xFF4B7284) else Color.Transparent
    val borderColor = if (selected) Color.Transparent else Color(0xFF637381)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (selected) 0.dp else 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFFF3F6F8),
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

private val ViewFilter.label: String
    get() = when (this) {
        ViewFilter.UNVIEWED_ONLY -> "Unread"
        ViewFilter.ALL -> "All"
        ViewFilter.VIEWED -> "Read"
    }
