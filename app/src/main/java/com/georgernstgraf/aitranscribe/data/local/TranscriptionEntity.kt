package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val retryCount: Int = 0
)