package com.georgernstgraf.aitranscribe.domain.model

enum class TranscriptionStatus {
    PENDING,
    PROCESSING,
    NO_NETWORK,
    STT_ERROR_RETRYABLE,
    STT_ERROR_PERMANENT,
    COMPLETED,
    COMPLETED_WITH_WARNING,
    FAILED
}
