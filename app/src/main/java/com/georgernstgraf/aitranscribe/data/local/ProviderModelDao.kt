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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCapabilities(capabilities: List<CapabilityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModelCapabilities(modelCapabilities: List<ModelCapabilityEntity>)

    @Query("DELETE FROM models WHERE provider_id = :providerId")
    suspend fun deleteModelsForProvider(providerId: String)

    @Query("DELETE FROM model_capabilities WHERE model_id IN (SELECT id FROM models WHERE provider_id = :providerId)")
    suspend fun deleteModelCapabilitiesForProvider(providerId: String)

    @Query("SELECT id FROM models WHERE provider_id = :providerId AND external_id = :externalId LIMIT 1")
    suspend fun getModelInternalId(providerId: String, externalId: String): Long?

    // Synchronization
    @Transaction
    suspend fun replaceModelsForProvider(providerId: String, newModels: List<ModelCatalogEntry>, timestamp: Long) {
        deleteModelCapabilitiesForProvider(providerId)
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

            val allCapabilities = newModels.flatMap { it.capabilities }.distinctBy { it.id }
            if (allCapabilities.isNotEmpty()) {
                insertCapabilities(allCapabilities)
            }

            val capabilityLinks = mutableListOf<ModelCapabilityEntity>()
            for (model in newModels) {
                val internalId = getModelInternalId(providerId, model.externalId) ?: continue
                capabilityLinks += model.capabilities.map {
                    ModelCapabilityEntity(
                        modelId = internalId,
                        capabilityId = it.id,
                        source = "api_architecture"
                    )
                }
            }
            if (capabilityLinks.isNotEmpty()) {
                insertModelCapabilities(capabilityLinks)
            }
        }

        updateProviderSyncTimestamp(providerId, timestamp)
    }
}
