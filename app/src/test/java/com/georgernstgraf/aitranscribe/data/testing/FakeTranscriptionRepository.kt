package com.georgernstgraf.aitranscribe.data.testing

import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake implementation of TranscriptionRepository for testing.
 * Uses in-memory storage for fast, isolated tests.
 */
class FakeTranscriptionRepository : TranscriptionRepository {

    private val transcriptions = MutableStateFlow<List<TranscriptionEntity>>(emptyList())
    private val queuedTranscriptions = MutableStateFlow<List<QueuedTranscriptionEntity>>(emptyList())

    override suspend fun insert(transcription: TranscriptionEntity): Long {
        val newId = (transcriptions.value.maxOfOrNull { it.id } ?: 0) + 1
        val newEntity = transcription.copy(id = newId)
        transcriptions.value = transcriptions.value + newEntity
        return newId
    }

    override suspend fun insertAll(transcriptions: List<TranscriptionEntity>) {
        val newTranscriptions = transcriptions.map { t ->
            val newId = (this.transcriptions.value.maxOfOrNull { it.id } ?: 0L) + 1L
            t.copy(id = newId)
        }
        this.transcriptions.value = this.transcriptions.value + newTranscriptions
    }

    override suspend fun update(transcription: TranscriptionEntity) {
        transcriptions.value = transcriptions.value.map {
            if (it.id == transcription.id) transcription else it
        }
    }

    override suspend fun deleteById(id: Long): Int {
        transcriptions.value = transcriptions.value.filterNot { it.id == id }
        return 1
    }

    override suspend fun deleteOld(cutoffDate: String, viewFilter: ViewFilter): Int {
        val filtered = if (viewFilter == ViewFilter.ALL) {
            transcriptions.value.filter { it.createdAt < cutoffDate }
        } else {
            transcriptions.value.filter { it.createdAt < cutoffDate && it.playedCount == 0 }
        }
        transcriptions.value = transcriptions.value.filterNot { it in filtered }
        return filtered.size
    }

    override suspend fun deleteByIds(ids: List<Long>): Int {
        transcriptions.value = transcriptions.value.filterNot { it.id in ids }
        return ids.size
    }

    override suspend fun getById(id: Long): TranscriptionEntity? {
        return transcriptions.value.find { it.id == id }
    }

    override fun getByIdFlow(id: Long): Flow<TranscriptionEntity?> {
        return transcriptions.map { list -> list.find { it.id == id } }
    }

    override fun searchTranscriptions(
        startDate: String?,
        endDate: String?,
        searchQuery: String?,
        viewFilter: ViewFilter
    ): Flow<List<Transcription>> {
        return transcriptions.map { entities ->
            entities
                .filter {
                    (startDate == null || it.createdAt >= startDate) &&
                    (endDate == null || it.createdAt <= endDate) &&
                    (searchQuery == null ||
                        it.originalText.contains(searchQuery, ignoreCase = true) ||
                        (it.processedText?.contains(searchQuery, ignoreCase = true) == true))
                }
                .filter {
                    viewFilter == ViewFilter.ALL || it.playedCount == 0
                }
                .map { it.toDomain() }
        }
    }

    override fun getByStatus(status: String, limit: Int): Flow<List<Transcription>> {
        return transcriptions.map { entities ->
            entities
                .filter { it.status == status }
                .take(limit)
                .map { it.toDomain() }
        }
    }

    override fun getUnviewed(limit: Int): Flow<List<Transcription>> {
        return transcriptions.map { entities ->
            entities
                .filter { it.playedCount == 0 }
                .take(limit)
                .map { it.toDomain() }
        }
    }

    override suspend fun markAsViewed(id: Long): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(playedCount = it.playedCount + 1) else it
        }
        return 1
    }

    override suspend fun resetViewStatus(id: Long): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(playedCount = 0) else it
        }
        return 1
    }

    override suspend fun updateStatus(id: Long, status: String): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(status = status) else it
        }
        return 1
    }

    override suspend fun recordError(id: Long, error: String): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(
                errorMessage = error,
                retryCount = it.retryCount + 1
            ) else it
        }
        return 1
    }

    override suspend fun getCount(): Int {
        return transcriptions.value.size
    }

    override suspend fun getOldCount(cutoffDate: String, viewFilter: ViewFilter): Int {
        return if (viewFilter == ViewFilter.ALL) {
            transcriptions.value.count { it.createdAt < cutoffDate }
        } else {
            transcriptions.value.count { it.createdAt < cutoffDate && it.playedCount == 0 }
        }
    }

    override suspend fun queueForOffline(queued: QueuedTranscriptionEntity): Long {
        val newId = (queuedTranscriptions.value.maxOfOrNull { it.id } ?: 0) + 1
        val newEntity = queued.copy(id = newId)
        queuedTranscriptions.value = queuedTranscriptions.value + newEntity
        return newId
    }

    override suspend fun getQueuedById(id: Long): QueuedTranscriptionEntity? {
        return queuedTranscriptions.value.find { it.id == id }
    }

    override fun getAllQueued(): Flow<List<QueuedTranscriptionEntity>> {
        return queuedTranscriptions
    }

    override suspend fun removeQueued(id: Long): Int {
        queuedTranscriptions.value = queuedTranscriptions.value.filterNot { it.id == id }
        return 1
    }

    override suspend fun clearQueue(): Int {
        val count = queuedTranscriptions.value.size
        queuedTranscriptions.value = emptyList()
        return count
    }

    override suspend fun getQueueCount(): Int {
        return queuedTranscriptions.value.size
    }

    fun clear() {
        transcriptions.value = emptyList()
        queuedTranscriptions.value = emptyList()
    }

    private fun TranscriptionEntity.toDomain(): Transcription {
        return Transcription(
            id = id,
            originalText = originalText,
            processedText = processedText,
            audioFilePath = audioFilePath,
            createdAt = java.time.LocalDateTime.parse(createdAt),
            postProcessingType = postProcessingType?.let {
                com.georgernstgraf.aitranscribe.domain.model.PostProcessingType.valueOf(it)
            },
            status = com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus.valueOf(status),
            errorMessage = errorMessage,
            playedCount = playedCount,
            retryCount = retryCount
        )
    }
}
