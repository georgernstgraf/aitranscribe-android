package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for queuing transcriptions for offline processing.
 * Stores transcription options for later retry when network is available.
 */
@ViewModelScoped
class QueueOfflineTranscriptionUseCase @Inject constructor(
    private val repository: TranscriptionRepository
) {

    suspend operator fun invoke(
        audioPath: String,
        sttModel: String,
        llmModel: String?,
        postProcessingType: String?
    ) = withContext(Dispatchers.IO) {
        if (audioPath.isBlank()) {
            throw QueueException("Audio path cannot be empty")
        }

        if (sttModel.isBlank()) {
            throw QueueException("STT model cannot be empty")
        }

        val queued = TranscriptionEntity(
            originalText = "",
            processedText = null,
            audioFilePath = audioPath,
            sttModel = sttModel,
            llmModel = llmModel,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            postProcessingType = postProcessingType,
            status = TranscriptionStatus.NO_NETWORK.name,
            errorMessage = "No network available",
            playedCount = 0,
            retryCount = 0,
            summary = null
        )

        repository.insert(queued)
    }

    class QueueException(message: String) : Exception(message)
}
