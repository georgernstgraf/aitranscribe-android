package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderModelDao {

    // Providers
    @Query("SELECT * FROM providers")
    fun getAllProvidersFlow(): Flow<List<ProviderEntity>>

    @Query("SELECT * FROM providers")
    suspend fun getAllProviders(): List<ProviderEntity>

    @Query("SELECT * FROM providers WHERE id = :providerId")
    suspend fun getProviderById(providerId: String): ProviderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviders(providers: List<ProviderEntity>)

    @Query("UPDATE providers SET last_synced_at = :timestamp WHERE id = :providerId")
    suspend fun updateProviderSyncTimestamp(providerId: String, timestamp: Long)

    // Models
    @Query("SELECT * FROM models WHERE provider_id = :providerId ORDER BY model_name ASC")
    fun getModelsForProviderFlow(providerId: String): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE provider_id = :providerId ORDER BY model_name ASC")
    suspend fun getModelsForProvider(providerId: String): List<ModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)

    @Query("DELETE FROM models WHERE provider_id = :providerId")
    suspend fun deleteModelsForProvider(providerId: String)

    // Synchronization
    @Transaction
    suspend fun replaceModelsForProvider(providerId: String, newModels: List<ModelEntity>, timestamp: Long) {
        deleteModelsForProvider(providerId)
        insertModels(newModels)
        updateProviderSyncTimestamp(providerId, timestamp)
    }
}
