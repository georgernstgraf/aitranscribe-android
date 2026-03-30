package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
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
    @ColumnInfo(name = "post_processing_type")
    val postProcessingType: String?,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "error_message")
    val errorMessage: String?,
    @ColumnInfo(name = "played_count")
    val playedCount: Int = 0,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
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
        postProcessingType = postProcessingType?.toPostProcessingTypeOrNull(),
        status = TranscriptionStatus.valueOf(status),
        errorMessage = errorMessage,
        playedCount = playedCount,
        retryCount = retryCount,
        summary = summary
    )
}

private fun String.toPostProcessingTypeOrNull(): PostProcessingType? {
    return when (this) {
        "ENGLISH" -> PostProcessingType.TRANSLATE_TO_EN
        else -> runCatching { PostProcessingType.valueOf(this) }.getOrNull()
    }
}
