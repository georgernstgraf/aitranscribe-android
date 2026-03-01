package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for resetting view status.
 * Sets played_count back to 0 (makes transcription unviewed again).
 */
@ViewModelScoped
class ResetViewStatusUseCase @Inject constructor(
    private val repository: TranscriptionRepository
) {

    suspend operator fun invoke(id: Long) = withContext(Dispatchers.IO) {
        val result = repository.resetViewStatus(id)
        
        if (result == 0) {
            throw ResetViewStatusException("Transcription not found: $id")
        }
    }

    class ResetViewStatusException(message: String) : Exception(message)
}