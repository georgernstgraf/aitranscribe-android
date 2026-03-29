package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Enhanced DAO with bulk operations and optimizations.
 */
@Dao
interface TranscriptionDaoEnhanced {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: TranscriptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transcriptions: List<TranscriptionEntity>): List<Long>

    @Update
    suspend fun update(transcription: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM transcriptions WHERE created_at < :cutoffDate AND (:viewFilter = 'ALL' OR (:viewFilter = 'UNVIEWED_ONLY' AND played_count = 0) OR (:viewFilter = 'VIEWED' AND played_count > 0))")
    suspend fun deleteOld(cutoffDate: String, viewFilter: String): Int

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    suspend fun getById(id: Long): TranscriptionEntity?

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<TranscriptionEntity?>

    @Query("""
        SELECT * FROM transcriptions 
        WHERE 
            (:startDate IS NULL OR created_at >= :startDate)
            AND (:endDate IS NULL OR created_at <= :endDate)
            AND (:searchQuery IS NULL OR 
                 original_text LIKE '%' || :searchQuery || '%' OR 
                 processed_text LIKE '%' || :searchQuery || '%')
            AND (:viewFilter = 'ALL' OR played_count = 0)
        ORDER BY created_at DESC
    """)
    fun searchTranscriptions(
        startDate: String?,
        endDate: String?,
        searchQuery: String?,
        viewFilter: String
    ): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE status = :status ORDER BY created_at DESC LIMIT :limit")
    fun getByStatus(status: String, limit: Int): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions WHERE played_count = 0 ORDER BY created_at DESC LIMIT :limit")
    fun getUnviewed(limit: Int): Flow<List<TranscriptionEntity>>

    @Query("UPDATE transcriptions SET played_count = played_count + 1 WHERE id = :id")
    suspend fun incrementPlayedCount(id: Long): Int

    @Query("UPDATE transcriptions SET played_count = 0 WHERE id = :id")
    suspend fun resetPlayedCount(id: Long): Int

    @Query("UPDATE transcriptions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String): Int

    @Query("UPDATE transcriptions SET error_message = :error, retry_count = retry_count + 1 WHERE id = :id")
    suspend fun recordError(id: Long, error: String): Int

    @Query("SELECT COUNT(*) FROM transcriptions")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM transcriptions WHERE created_at < :cutoffDate AND (:viewFilter = 'ALL' OR (:viewFilter = 'UNVIEWED_ONLY' AND played_count = 0) OR (:viewFilter = 'VIEWED' AND played_count > 0))")
    suspend fun getOldCount(cutoffDate: String, viewFilter: String): Int

    @Query("UPDATE transcriptions SET played_count = played_count + 1 WHERE played_count = 0 LIMIT :limit")
    suspend fun markAllAsViewed(limit: Int): Int

    @Query("UPDATE transcriptions SET played_count = 0 LIMIT :limit")
    suspend fun markAllAsUnviewed(limit: Int): Int

    @Query("DELETE FROM transcriptions WHERE id IN (SELECT id FROM transcriptions LIMIT :limit)")
    suspend fun deleteOldest(limit: Int): Int

    @Query("SELECT COUNT(*) FROM transcriptions WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("""
        SELECT * FROM transcriptions 
        WHERE 
            status = :status 
            AND created_at >= :afterDate
            AND created_at <= :beforeDate
        ORDER BY created_at DESC
    """)
    fun getByStatusAndDateRange(
        status: String,
        afterDate: String,
        beforeDate: String
    ): Flow<List<TranscriptionEntity>>

    @Query("SELECT * FROM transcriptions ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandom(limit: Int): List<TranscriptionEntity>

    @Query("SELECT AVG(LENGTH(original_text)) FROM transcriptions")
    suspend fun getAverageTranscriptionLength(): Double?

    @Query("SELECT COUNT(*) FROM transcriptions WHERE processed_text IS NOT NULL")
    suspend fun getProcessedCount(): Int
}