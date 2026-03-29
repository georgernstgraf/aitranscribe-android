package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import java.time.format.DateTimeFormatter

@Composable
fun TranscriptionItem(
    transcription: Transcription,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnviewed = transcription.isUnviewed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnviewed) {
                Color(0xFF1E3044)
            } else {
                Color(0xFF172028)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = transcription.summary ?: getPreviewText(transcription),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                    color = if (isUnviewed) Color(0xFFF3F6F8) else Color(0xFFB0BEC5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = Color(0xFF8FA2B2),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun formatDateTime(dateTime: java.time.LocalDateTime): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return formatter.format(dateTime)
}

private fun getPreviewText(transcription: Transcription): String {
    val text = transcription.processedText ?: transcription.originalText
    return if (text.length > 60) {
        text.take(60) + "…"
    } else {
        text
    }
}
