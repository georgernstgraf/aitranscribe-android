package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georgernstgraf.aitranscribe.ui.theme.Red

@Composable
fun AudioRecordingButton(
    isRecording: Boolean,
    recordingDuration: Int,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recordingColor = if (isRecording) Red else MaterialTheme.colorScheme.primary
    val icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic

    Box(
        modifier = modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(recordingColor)
            .pointerInput(isRecording) {
                detectTapGestures(
                    onPress = {
                        if (!isRecording) {
                            onStartRecording()
                            val released = tryAwaitRelease()
                            if (released) {
                                onStopRecording()
                            }
                        } else {
                            onStopRecording()
                        }
                    }
                )
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
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "$recordingDuration",
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = "Start Recording",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
