package com.georgernstgraf.aitranscribe.domain.model

enum class TranscriptionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    COMPLETED_WITH_WARNING,
    FAILED
}