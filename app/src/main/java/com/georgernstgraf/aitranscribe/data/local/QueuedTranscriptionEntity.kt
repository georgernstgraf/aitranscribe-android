package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queued_transcriptions")
data class QueuedTranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val audioFilePath: String,
    val sttModel: String,
    val llmModel: String?,
    val postProcessingType: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    val priority: Int = 0
)