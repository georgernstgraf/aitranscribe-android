package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QueuedTranscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(queued: QueuedTranscriptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(queuedList: List<QueuedTranscriptionEntity>)

    @Query("DELETE FROM queued_transcriptions WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM queued_transcriptions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QueuedTranscriptionEntity?

    @Query("""
        SELECT * FROM queued_transcriptions 
        ORDER BY created_at ASC, priority DESC
        LIMIT 1
    """)
    fun getNextFlow(): Flow<QueuedTranscriptionEntity?>

    @Query("""
        SELECT * FROM queued_transcriptions
        ORDER BY created_at ASC, priority DESC
    """)
    fun getAll(): Flow<List<QueuedTranscriptionEntity>>

    @Query("SELECT COUNT(*) FROM queued_transcriptions")
    suspend fun getCount(): Int

    @Query("SELECT audioFilePath FROM queued_transcriptions")
    suspend fun getAllAudioPaths(): List<String>

    @Query("DELETE FROM queued_transcriptions")
    suspend fun clearAll(): Int
}