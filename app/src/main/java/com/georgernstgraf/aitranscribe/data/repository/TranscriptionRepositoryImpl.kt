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

    override fun getByStatus(status: String, limit: Int): Flow<List<Transcription>> {
        return transcriptionDao.getByStatus(status, limit)
            .map { entities -> entities.map { it.toDomain() } }
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

    override suspend fun updateStatus(id: Long, status: String): Int {
        return transcriptionDao.updateStatus(id, status)
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

    override suspend fun clearAudioPath(id: Long) {
        transcriptionDao.clearAudioPath(id)
    }

    override suspend fun getByStatuses(statuses: List<String>): List<TranscriptionEntity> {
        return transcriptionDao.getByStatuses(statuses)
    }

    override suspend fun updateStatusAndError(id: Long, status: String, errorMessage: String?) {
        transcriptionDao.updateStatusAndError(id, status, errorMessage)
    }

    override suspend fun updateSttModel(id: Long, sttModel: String) {
        transcriptionDao.updateSttModel(id, sttModel)
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
