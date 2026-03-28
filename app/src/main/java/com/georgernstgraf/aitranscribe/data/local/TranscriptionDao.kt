package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: TranscriptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transcriptions: List<TranscriptionEntity>)

    @Update
    suspend fun update(transcription: TranscriptionEntity)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("""
        DELETE FROM transcriptions 
        WHERE created_at < :cutoffDate 
        AND (:viewFilter = 'ALL' OR played_count = 0)
    """)
    suspend fun deleteOld(cutoffDate: String, viewFilter: String): Int

    @Query("DELETE FROM transcriptions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>): Int

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

    @Query("""
        SELECT * FROM transcriptions 
        WHERE status = :status
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun getByStatus(status: String, limit: Int = 10): Flow<List<TranscriptionEntity>>

    @Query("""
        SELECT * FROM transcriptions 
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun getAllTranscriptions(limit: Int = 10): Flow<List<TranscriptionEntity>>

    @Query("""
        SELECT * FROM transcriptions 
        WHERE played_count = 0
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun getUnviewed(limit: Int = 10): Flow<List<TranscriptionEntity>>

    @Query("""
        SELECT * FROM transcriptions 
        WHERE played_count > 0
        ORDER BY created_at DESC
        LIMIT :limit
    """)
    fun getViewed(limit: Int = 10): Flow<List<TranscriptionEntity>>

    @Query("""
        UPDATE transcriptions 
        SET played_count = played_count + 1 
        WHERE id = :id
    """)
    suspend fun incrementPlayedCount(id: Long): Int

    @Query("""
        UPDATE transcriptions 
        SET played_count = 0 
        WHERE id = :id
    """)
    suspend fun resetPlayedCount(id: Long): Int

    @Query("""
        UPDATE transcriptions 
        SET status = :status 
        WHERE id = :id
    """)
    suspend fun updateStatus(id: Long, status: String): Int

    @Query("""
        UPDATE transcriptions 
        SET error_message = :error, retry_count = retry_count + 1 
        WHERE id = :id
    """)
    suspend fun recordError(id: Long, error: String): Int

    @Query("SELECT COUNT(*) FROM transcriptions")
    suspend fun getCount(): Int

    @Query("""
        SELECT COUNT(*) FROM transcriptions 
        WHERE created_at < :cutoffDate 
        AND (:viewFilter = 'ALL' OR played_count = 0)
    """)
    suspend fun getOldCount(cutoffDate: String, viewFilter: String): Int
}