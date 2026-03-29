package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
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

    fun getByStatus(status: String, limit: Int): Flow<List<Transcription>>

    fun getAllTranscriptions(limit: Int): Flow<List<Transcription>>

    fun getUnviewed(limit: Int): Flow<List<Transcription>>

    fun getViewed(limit: Int): Flow<List<Transcription>>

    suspend fun markAsViewed(id: Long): Int

    suspend fun resetViewStatus(id: Long): Int

    suspend fun updateStatus(id: Long, status: String): Int

    suspend fun recordError(id: Long, error: String): Int

    suspend fun getCount(): Int

    suspend fun getOldCount(cutoffDate: String, viewFilter: ViewFilter): Int

    suspend fun updateSummary(id: Long, summary: String)

    suspend fun queueForOffline(queued: QueuedTranscriptionEntity): Long

    suspend fun getQueuedById(id: Long): QueuedTranscriptionEntity?

    fun getAllQueued(): Flow<List<QueuedTranscriptionEntity>>

    suspend fun removeQueued(id: Long): Int

    suspend fun clearQueue(): Int

    suspend fun getQueueCount(): Int

    suspend fun getQueuedAudioPaths(): List<String>

    suspend fun getNextTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long?

    suspend fun getPrevTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long?
}