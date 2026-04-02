package com.georgernstgraf.aitranscribe.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.ModelCatalogEntry
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class ModelSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val providerModelDao: ProviderModelDao,
    private val groqApiService: GroqApiService,
    private val openRouterApiService: OpenRouterApiService,
    private val zaiApiService: ZaiApiService,
    private val appSettingsStore: AppSettingsStore
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ModelSyncWorker"
        private const val SYNC_INTERVAL_MS = 12 * 60 * 60 * 1000L // 12 hours
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting model synchronization")
        try {
            val providers = providerModelDao.getAllProviders()
            val now = System.currentTimeMillis()

            for (provider in providers) {
                if (now - provider.lastSyncedAt > SYNC_INTERVAL_MS) {
                    syncProvider(provider.id, now)
                } else {
                    Log.d(TAG, "Skipping sync for ${provider.id}, last synced recently.")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during model synchronization", e)
            Result.retry()
        }
    }

    private suspend fun syncProvider(providerId: String, timestamp: Long) {
        var token = providerModelDao.getProviderApiToken(providerId)
        if (token.isNullOrBlank()) {
            token = appSettingsStore.getActiveAuthToken(providerId)
            if (!token.isNullOrBlank()) {
                providerModelDao.updateProviderApiToken(providerId, token)
            }
        }
        val bearerToken = token?.let { if (it.startsWith("Bearer ")) it else "Bearer $it" }

        try {
            val models = when (providerId) {
                "groq" -> {
                    if (bearerToken == null) {
                        Log.d(TAG, "Skipping Groq sync, no token")
                        return
                    }
                    val response = groqApiService.getModels(bearerToken)
                    if (response.isSuccessful) {
                        response.body()?.data?.map {
                            ModelCatalogEntry(
                                externalId = it.id,
                                modelName = it.name ?: it.id
                            )
                        } ?: emptyList()
                    } else emptyList()
                }
                "openrouter" -> {
                    // OpenRouter allows fetching models without auth
                    val response = openRouterApiService.getModels()
                    if (response.isSuccessful) {
                        response.body()?.data?.map {
                            ModelCatalogEntry(
                                externalId = it.id,
                                modelName = it.name ?: it.id
                            )
                        } ?: emptyList()
                    } else emptyList()
                }
                "zai" -> {
                    if (bearerToken == null) {
                        Log.d(TAG, "Skipping ZAI sync, no token")
                        return
                    }
                    val response = zaiApiService.getModels(bearerToken)
                    if (response.isSuccessful) {
                        response.body()?.data?.map {
                            ModelCatalogEntry(
                                externalId = it.id,
                                modelName = it.name ?: it.id
                            )
                        } ?: emptyList()
                    } else emptyList()
                }
                else -> emptyList()
            }

            if (models.isNotEmpty()) {
                Log.d(TAG, "Successfully fetched ${models.size} models for $providerId")
                providerModelDao.replaceModelsForProvider(providerId, models, timestamp)
            } else {
                Log.w(TAG, "Fetched 0 models for $providerId or request failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync models for $providerId", e)
            // Do not throw here, let other providers try to sync
        }
    }
}
