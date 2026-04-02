package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import java.time.LocalDateTime

@Entity(
    tableName = "transcriptions",
    foreignKeys = [
        ForeignKey(
            entity = LanguageEntity::class,
            parentColumns = ["id"],
            childColumns = ["languagesId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["languagesId"])
    ]
)
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "stt_text")
    val sttText: String?,

    @ColumnInfo(name = "cleaned_text")
    val cleanedText: String?,

    @ColumnInfo(name = "audio_file_path")
    val audioFilePath: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @ColumnInfo(name = "error_message")
    val errorMessage: String?,

    @ColumnInfo(name = "seen")
    val seen: Boolean = false,

    @ColumnInfo(name = "summary")
    val summary: String? = null,

    @ColumnInfo(name = "languagesId")
    val languageId: String? = null
)

fun TranscriptionEntity.toDomain(): Transcription {
    return Transcription(
        id = id,
        sttText = sttText,
        cleanedText = cleanedText,
        audioFilePath = audioFilePath,
        createdAt = LocalDateTime.parse(createdAt),
        errorMessage = errorMessage,
        seen = seen,
        summary = summary,
        language = languageId
    )
}
