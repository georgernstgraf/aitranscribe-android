package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
                Color(0xFF203040)
            } else {
                Color(0xFF1A2733)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isUnviewed) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(5.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .background(Color(0xFFD7A928))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (isUnviewed) 13.dp else 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = transcription.summary ?: getPreviewText(transcription),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = Color(0xFFF3F6F8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = formatDateTime(transcription.createdAt),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                        color = Color(0xFF9FB0BE)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ViewedBadge(isViewed = transcription.isViewed)

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = Color(0xFF8FA2B2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ViewedBadge(isViewed: Boolean, modifier: Modifier = Modifier) {
    val bgColor = if (isViewed) Color(0xFF2D6A4F) else Color(0xFF8A6A16)
    val textColor = if (isViewed) Color(0xFFEAF7EF) else Color(0xFFFFF7DA)
    val label = if (isViewed) "Viewed" else "Unviewed"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
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
