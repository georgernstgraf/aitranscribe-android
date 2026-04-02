package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {

    suspend fun insert(transcription: TranscriptionEntity): Long

    suspend fun insertAll(transcriptions: List<TranscriptionEntity>)

    suspend fun update(transcription: TranscriptionEntity)

    suspend fun deleteById(id: Long): Int

    suspend fun deleteOld(cutoffDate: String, viewFilter: ViewFilter): Int

    suspend fun deleteByIds(ids: List<Long>): Int

    suspend fun getById(id: Long): TranscriptionEntity?

    fun getByIdFlow(id: Long): Flow<TranscriptionEntity?>

    fun searchTranscriptions(
        startDate: String?,
        endDate: String?,
        searchQuery: String?,
        viewFilter: ViewFilter
    ): Flow<List<Transcription>>

    fun getAllTranscriptions(limit: Int): Flow<List<Transcription>>

    fun getUnviewed(limit: Int): Flow<List<Transcription>>

    fun getViewed(limit: Int): Flow<List<Transcription>>

    suspend fun markAsViewed(id: Long): Int

    suspend fun resetViewStatus(id: Long): Int

    suspend fun recordError(id: Long, error: String): Int

    suspend fun getCount(): Int

    suspend fun getOldCount(cutoffDate: String, viewFilter: ViewFilter): Int

    suspend fun updateSummary(id: Long, summary: String)

    suspend fun updateCleanedText(id: Long, cleanedText: String)

    suspend fun updateLanguage(id: Long, languageId: String)

    suspend fun getUnfinishedSttTranscriptions(): List<TranscriptionEntity>

    suspend fun markSttSuccess(id: Long, sttText: String, languageId: String?): Int

    suspend fun getAllAudioPaths(): List<String>

    suspend fun getNextTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long?

    suspend fun getPrevTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long?

    fun getFilteredIds(viewFilter: ViewFilter): Flow<List<Long>>

    suspend fun clearAudioPath(id: Long)

    suspend fun markAudioMissing(id: Long, errorMessage: String)
}
