package com.georgernstgraf.aitranscribe.domain.model

import java.time.LocalDateTime

data class Transcription(
    val id: Long = 0,
    val originalText: String,
    val processedText: String?,
    val audioFilePath: String?,
    val createdAt: LocalDateTime,
    val postProcessingType: PostProcessingType?,
    val status: TranscriptionStatus,
    val errorMessage: String?,
    val playedCount: Int = 0,
    val seen: Boolean = false,
    val retryCount: Int = 0,
    val summary: String? = null
) {
    val isViewed: Boolean
        get() = seen

    val isUnviewed: Boolean
        get() = !seen

    fun getShareText(): String {
        return processedText ?: originalText
    }

    fun getShareTitle(): String {
        return summary?.takeIf { it.isNotBlank() } ?: "Transcription from AITranscribe"
    }
}
