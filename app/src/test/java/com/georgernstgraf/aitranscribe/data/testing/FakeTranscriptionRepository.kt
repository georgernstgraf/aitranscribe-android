package com.georgernstgraf.aitranscribe.data.testing

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * Fake implementation of TranscriptionRepository for testing.
 * Uses in-memory storage for fast, isolated tests.
 */
class FakeTranscriptionRepository : TranscriptionRepository {

    private val transcriptions = MutableStateFlow<List<TranscriptionEntity>>(emptyList())

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
            transcriptions.value.filter { it.createdAt < cutoffDate && !it.seen }
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
                        (it.sttText?.contains(searchQuery, ignoreCase = true) == true) ||
                        (it.cleanedText?.contains(searchQuery, ignoreCase = true) == true))
                }
                .filter {
                    viewFilter == ViewFilter.ALL || !it.seen
                }
                .map { it.toDomain() }
        }
    }

    override fun getAllTranscriptions(limit: Int): Flow<List<Transcription>> {
        return transcriptions.map { entities ->
            entities
                .take(limit)
                .map { it.toDomain() }
        }
    }

    override fun getUnviewed(limit: Int): Flow<List<Transcription>> {
        return transcriptions.map { entities ->
            entities
                .filter { !it.seen }
                .take(limit)
                .map { it.toDomain() }
        }
    }

    override fun getViewed(limit: Int): Flow<List<Transcription>> {
        return transcriptions.map { entities ->
            entities
                .filter { it.seen }
                .take(limit)
                .map { it.toDomain() }
        }
    }

    override suspend fun markAsViewed(id: Long): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(seen = true) else it
        }
        return 1
    }

    override suspend fun resetViewStatus(id: Long): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(seen = false) else it
        }
        return 1
    }

    override suspend fun recordError(id: Long, error: String): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(errorMessage = error) else it
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
            transcriptions.value.count { it.createdAt < cutoffDate && !it.seen }
        }
    }

    override suspend fun getUnfinishedSttTranscriptions(): List<TranscriptionEntity> {
        return transcriptions.value.filter { it.sttText == null && it.audioFilePath != null }
    }

    override suspend fun markSttSuccess(id: Long, sttText: String, languageId: String?): Int {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) {
                it.copy(
                    sttText = sttText,
                    audioFilePath = null,
                    languageId = languageId,
                    errorMessage = null
                )
            } else {
                it
            }
        }
        return 1
    }

    override suspend fun updateSummary(id: Long, summary: String) {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(summary = summary) else it
        }
    }

    override suspend fun updateCleanedText(id: Long, cleanedText: String) {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(cleanedText = cleanedText) else it
        }
    }

    override suspend fun updateLanguage(id: Long, languageId: String) {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(languageId = languageId) else it
        }
    }

    override suspend fun clearAudioPath(id: Long) {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(audioFilePath = null) else it
        }
    }

    override suspend fun markAudioMissing(id: Long, errorMessage: String) {
        transcriptions.value = transcriptions.value.map {
            if (it.id == id) it.copy(audioFilePath = null, errorMessage = errorMessage) else it
        }
    }

    override suspend fun getAllAudioPaths(): List<String> {
        return transcriptions.value.mapNotNull { it.audioFilePath }
    }

    override suspend fun getNextTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long? {
        val sorted = transcriptions.value.sortedBy { it.id }
        val idx = sorted.indexOfFirst { it.id == currentId }
        return if (idx >= 0 && idx < sorted.lastIndex) sorted[idx + 1].id else null
    }

    override suspend fun getPrevTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long? {
        val sorted = transcriptions.value.sortedBy { it.id }
        val idx = sorted.indexOfFirst { it.id == currentId }
        return if (idx > 0) sorted[idx - 1].id else null
    }

    fun clear() {
        transcriptions.value = emptyList()
    }

    override fun getFilteredIds(viewFilter: ViewFilter): Flow<List<Long>> {
        val ids = transcriptions.value
            .filter { viewFilter == ViewFilter.ALL || !it.seen }
            .map { it.id }
        return kotlinx.coroutines.flow.flowOf(ids)
    }

    private fun TranscriptionEntity.toDomain(): Transcription {
        return Transcription(
            id = id,
            sttText = sttText,
            cleanedText = cleanedText,
            audioFilePath = audioFilePath,
            createdAt = LocalDateTime.parse(createdAt),
            errorMessage = errorMessage,
            seen = seen,
            summary = summary,
            language = languageId
        )
    }
}
