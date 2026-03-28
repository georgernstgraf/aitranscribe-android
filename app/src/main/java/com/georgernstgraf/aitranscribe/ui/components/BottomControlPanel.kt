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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.ui.theme.Red

@Composable
fun BottomControlPanel(
    processingMode: PostProcessingType,
    onModeChanged: (PostProcessingType) -> Unit,
    isRecording: Boolean,
    recordingDuration: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF121C26))
            .border(1.dp, Color(0xFF243241), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterStart)
                .padding(end = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(
                label = "Raw Transcription",
                selected = processingMode == PostProcessingType.RAW,
                onClick = { onModeChanged(PostProcessingType.RAW) }
            )
            RadioButton(
                label = "Cleanup & Preserve Language",
                selected = processingMode == PostProcessingType.CLEANUP,
                onClick = { onModeChanged(PostProcessingType.CLEANUP) }
            )
            RadioButton(
                label = "Cleanup & Translate English",
                selected = processingMode == PostProcessingType.ENGLISH,
                onClick = { onModeChanged(PostProcessingType.ENGLISH) }
            )
        }

        RecordButton(
            isRecording = isRecording,
            recordingDuration = recordingDuration,
            onStartRecording = onStartRecording,
            onStopRecording = onStopRecording,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun RadioButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.5.dp, Color(0xFFF2F5F7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF69C3F0))
                )
            }
        }
        Text(
            text = label,
            color = Color(0xFFF2F5F7),
            fontSize = 14.sp
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
            .size(80.dp)
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
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = formatDuration(recordingDuration),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = "Start Recording",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
