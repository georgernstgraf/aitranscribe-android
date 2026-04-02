package com.georgernstgraf.aitranscribe.domain.model

import java.time.LocalDateTime

data class Transcription(
    val id: Long = 0,
    val sttText: String?,        // Raw STT output
    val cleanedText: String?,    // Post-processed text
    val audioFilePath: String?,
    val createdAt: LocalDateTime,
    val errorMessage: String?,
    val seen: Boolean = false,
    val summary: String? = null,
    val language: String? = null
) {
    val isViewed: Boolean
        get() = seen

    val isUnviewed: Boolean
        get() = !seen

    val isPending: Boolean
        get() = sttText == null && audioFilePath != null

    val displayText: String?
        get() = cleanedText?.takeIf { it.isNotBlank() } ?: sttText

    fun getShareText(): String {
        return displayText ?: ""
    }

    fun getShareTitle(): String {
        return summary?.takeIf { it.isNotBlank() } ?: "Transcription from AITranscribe"
    }
}
