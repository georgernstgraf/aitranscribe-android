package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.TranscriptionDao
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.toDomain
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepositoryImpl @Inject constructor(
    private val transcriptionDao: TranscriptionDao
) : TranscriptionRepository {

    override suspend fun insert(transcription: TranscriptionEntity): Long {
        return transcriptionDao.insert(transcription)
    }

    override suspend fun insertAll(transcriptions: List<TranscriptionEntity>) {
        transcriptionDao.insertAll(transcriptions)
    }

    override suspend fun update(transcription: TranscriptionEntity) {
        transcriptionDao.update(transcription)
    }

    override suspend fun deleteById(id: Long): Int {
        return transcriptionDao.deleteById(id)
    }

    override suspend fun deleteOld(cutoffDate: String, viewFilter: ViewFilter): Int {
        return transcriptionDao.deleteOld(cutoffDate, viewFilter.name)
    }

    override suspend fun deleteByIds(ids: List<Long>): Int {
        return transcriptionDao.deleteByIds(ids)
    }

    override suspend fun getById(id: Long): TranscriptionEntity? {
        return transcriptionDao.getById(id)
    }

    override fun getByIdFlow(id: Long): Flow<TranscriptionEntity?> {
        return transcriptionDao.getByIdFlow(id)
    }

    override fun searchTranscriptions(
        startDate: String?,
        endDate: String?,
        searchQuery: String?,
        viewFilter: ViewFilter
    ): Flow<List<Transcription>> {
        return transcriptionDao.searchTranscriptions(
            startDate,
            endDate,
            searchQuery,
            viewFilter.name
        ).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getAllTranscriptions(limit: Int): Flow<List<Transcription>> {
        return transcriptionDao.getAllTranscriptions(limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getUnviewed(limit: Int): Flow<List<Transcription>> {
        return transcriptionDao.getUnviewed(limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override fun getViewed(limit: Int): Flow<List<Transcription>> {
        return transcriptionDao.getViewed(limit)
            .map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun markAsViewed(id: Long): Int {
        return transcriptionDao.incrementPlayedCount(id)
    }

    override suspend fun resetViewStatus(id: Long): Int {
        return transcriptionDao.resetPlayedCount(id)
    }

    override suspend fun recordError(id: Long, error: String): Int {
        return transcriptionDao.recordError(id, error)
    }

    override suspend fun getCount(): Int {
        return transcriptionDao.getCount()
    }

    override suspend fun getOldCount(cutoffDate: String, viewFilter: ViewFilter): Int {
        return transcriptionDao.getOldCount(cutoffDate, viewFilter.name)
    }

    override suspend fun updateSummary(id: Long, summary: String) {
        transcriptionDao.updateSummary(id, summary)
    }

    override suspend fun updateCleanedText(id: Long, cleanedText: String) {
        transcriptionDao.updateCleanedText(id, cleanedText)
    }

    override suspend fun updateLanguage(id: Long, languageId: String) {
        transcriptionDao.updateLanguage(id, languageId)
    }

    override suspend fun clearAudioPath(id: Long) {
        transcriptionDao.clearAudioPath(id)
    }

    override suspend fun markAudioMissing(id: Long, errorMessage: String) {
        transcriptionDao.markAudioMissing(id, errorMessage)
    }

    override suspend fun getUnfinishedSttTranscriptions(): List<TranscriptionEntity> {
        return transcriptionDao.getUnfinishedSttTranscriptions()
    }

    override suspend fun markSttSuccess(id: Long, sttText: String, languageId: String?): Int {
        return transcriptionDao.markSttSuccess(id, sttText, languageId)
    }

    override suspend fun getAllAudioPaths(): List<String> {
        return transcriptionDao.getAllAudioPaths()
    }

    override suspend fun getNextTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long? {
        return transcriptionDao.getNextId(currentId, viewFilter.name)
    }

    override suspend fun getPrevTranscriptionId(currentId: Long, viewFilter: ViewFilter): Long? {
        return transcriptionDao.getPrevId(currentId, viewFilter.name)
    }

    override fun getFilteredIds(viewFilter: ViewFilter): Flow<List<Long>> {
        return transcriptionDao.getFilteredIds(viewFilter.name)
    }
}
