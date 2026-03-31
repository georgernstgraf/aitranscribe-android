package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import java.time.LocalDateTime

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "original_text")
    val originalText: String,
    @ColumnInfo(name = "processed_text")
    val processedText: String?,
    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
    @ColumnInfo(name = "seen")
    val seen: Boolean = false,
    @ColumnInfo(name = "summary")
    val summary: String? = null
)

fun TranscriptionEntity.toDomain(): Transcription {
    return Transcription(
        id = id,
        originalText = originalText,
        processedText = processedText,
        audioFilePath = audioFilePath,
        createdAt = LocalDateTime.parse(createdAt),
        status = TranscriptionStatus.valueOf(status),
        errorMessage = errorMessage,
        playedCount = if (seen) 1 else 0,
        seen = seen,
        summary = summary
    )
}
