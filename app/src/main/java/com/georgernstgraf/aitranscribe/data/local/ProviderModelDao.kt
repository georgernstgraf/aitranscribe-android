package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProviderModelDao {

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

    @Query("UPDATE providers SET api_token = :token WHERE id = :providerId")
    suspend fun updateProviderApiToken(providerId: String, token: String?)

    @Query("UPDATE providers SET api_token = NULL")
    suspend fun clearAllProviderApiTokens()

    @Query("SELECT api_token FROM providers WHERE id = :providerId LIMIT 1")
    suspend fun getProviderApiToken(providerId: String): String?

    @Query("SELECT * FROM models WHERE provider_id = :providerId ORDER BY model_name ASC")
    fun getModelsForProviderFlow(providerId: String): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE provider_id = :providerId ORDER BY model_name ASC")
    suspend fun getModelsForProvider(providerId: String): List<ModelEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>)

    @Query("DELETE FROM models WHERE provider_id = :providerId")
    suspend fun deleteModelsForProvider(providerId: String)

    @Transaction
    suspend fun replaceModelsForProvider(providerId: String, newModels: List<ModelCatalogEntry>, timestamp: Long) {
        deleteModelsForProvider(providerId)

        if (newModels.isNotEmpty()) {
            val modelsToInsert = newModels.map {
                ModelEntity(
                    externalId = it.externalId,
                    providerId = providerId,
                    modelName = it.modelName
                )
            }
            insertModels(modelsToInsert)
        }

        updateProviderSyncTimestamp(providerId, timestamp)
    }
}
