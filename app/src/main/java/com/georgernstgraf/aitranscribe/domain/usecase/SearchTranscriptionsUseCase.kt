package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for searching transcriptions.
 * Supports filtering by date, text, and view status.
 */
@ViewModelScoped
class SearchTranscriptionsUseCase @Inject constructor(
    private val repository: TranscriptionRepository
) {

    operator fun invoke(
        startDate: String? = null,
        endDate: String? = null,
        searchQuery: String? = null,
        viewFilter: ViewFilter = ViewFilter.UNVIEWED_ONLY
    ): Flow<List<com.georgernstgraf.aitranscribe.domain.model.Transcription>> {
        return repository.searchTranscriptions(
            startDate = startDate,
            endDate = endDate,
            searchQuery = searchQuery?.ifBlank { null },
            viewFilter = viewFilter
        )
    }

    private fun String.ifBlank(default: String?): String? {
        return if (isBlank()) default else this
    }
}