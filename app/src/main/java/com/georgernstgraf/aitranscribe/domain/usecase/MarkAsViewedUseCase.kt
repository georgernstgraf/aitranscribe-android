package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for marking a transcription as viewed.
 * Increments the played_count field.
 */
@ViewModelScoped
class MarkAsViewedUseCase @Inject constructor(
    private val repository: TranscriptionRepository
) {

    suspend operator fun invoke(id: Long) = withContext(Dispatchers.IO) {
        val result = repository.markAsViewed(id)
        
        if (result == 0) {
            throw MarkAsViewedException("Transcription not found: $id")
        }
    }

    class MarkAsViewedException(message: String) : Exception(message)
}