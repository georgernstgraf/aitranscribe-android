package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import javax.inject.Inject

class SetTranscriptionLanguageUseCase @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository
) {
    suspend operator fun invoke(transcriptionId: Long, languageId: String): Result<Unit> {
        return try {
            transcriptionRepository.updateLanguage(transcriptionId, languageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
