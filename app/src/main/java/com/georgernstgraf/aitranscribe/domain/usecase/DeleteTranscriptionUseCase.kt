package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for deleting transcriptions.
 * Supports single deletion and bulk deletion by age/view status.
 */
@ViewModelScoped
class DeleteTranscriptionUseCase @Inject constructor(
    private val repository: TranscriptionRepository
) {

    suspend operator fun invoke(
        mode: DeleteMode,
        transcriptionId: Long? = null,
        cutoffDate: String? = null,
        viewFilter: ViewFilter = ViewFilter.ALL,
        getCount: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        when (mode) {
            DeleteMode.SINGLE -> {
                if (transcriptionId == null) {
                    throw DeleteException("Transcription ID required for single delete")
                }
                repository.deleteById(transcriptionId)
            }

            DeleteMode.OLD_ALL, DeleteMode.OLD_UNVIEWED -> {
                if (cutoffDate.isNullOrBlank()) {
                    throw DeleteException("Cutoff date required for bulk delete")
                }
                
                if (getCount) {
                    repository.getOldCount(cutoffDate, viewFilter)
                } else {
                    repository.deleteOld(cutoffDate, viewFilter)
                }
            }
        }
    }

    suspend fun getCutoffDate(daysOld: Int): String {
        return LocalDateTime.now()
            .minusDays(daysOld.toLong())
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    class DeleteException(message: String) : Exception(message)
}