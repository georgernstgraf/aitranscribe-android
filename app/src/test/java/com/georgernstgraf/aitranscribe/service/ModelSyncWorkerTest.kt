package com.georgernstgraf.aitranscribe.service

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.ModelEntity
import com.georgernstgraf.aitranscribe.data.local.ProviderEntity
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.ModelDto
import com.georgernstgraf.aitranscribe.data.remote.dto.ModelsResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response

class ModelSyncWorkerTest {

    private lateinit var worker: ModelSyncWorker
    private lateinit var providerModelDao: ProviderModelDao
    private lateinit var groqApiService: GroqApiService
    private lateinit var openRouterApiService: OpenRouterApiService
    private lateinit var zaiApiService: ZaiApiService
    private lateinit var appSettingsStore: AppSettingsStore
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        providerModelDao = mockk(relaxed = true)
        groqApiService = mockk(relaxed = true)
        openRouterApiService = mockk(relaxed = true)
        zaiApiService = mockk(relaxed = true)
        appSettingsStore = mockk(relaxed = true)

        worker = ModelSyncWorker(
            context,
            workerParams,
            providerModelDao,
            groqApiService,
            openRouterApiService,
            zaiApiService,
            appSettingsStore
        )
    }

    @Test
    fun `skips sync if lastSyncedAt is recent`() = runBlocking {
        val recentTime = System.currentTimeMillis() - 1000 // 1 second ago
        coEvery { providerModelDao.getAllProviders() } returns listOf(
            ProviderEntity("groq", "Groq", recentTime)
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { groqApiService.getModels(any()) }
        coVerify(exactly = 0) { providerModelDao.replaceModelsForProvider(any(), any(), any()) }
    }

    @Test
    fun `syncs models if lastSyncedAt is old`() = runBlocking {
        val oldTime = System.currentTimeMillis() - (13 * 60 * 60 * 1000L) // 13 hours ago
        coEvery { providerModelDao.getAllProviders() } returns listOf(
            ProviderEntity("openrouter", "OpenRouter", oldTime)
        )
        
        val fakeModels = listOf(ModelDto(id = "test-model"))
        coEvery { openRouterApiService.getModels() } returns Response.success(ModelsResponse(fakeModels))

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) { openRouterApiService.getModels() }
        coVerify(exactly = 1) { providerModelDao.replaceModelsForProvider("openrouter", any(), any()) }
    }

    @Test
    fun `skips groq sync if no api key`() = runBlocking {
        val oldTime = System.currentTimeMillis() - (13 * 60 * 60 * 1000L)
        coEvery { providerModelDao.getAllProviders() } returns listOf(
            ProviderEntity("groq", "Groq", oldTime)
        )
        coEvery { appSettingsStore.getActiveAuthToken("groq") } returns null

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 0) { groqApiService.getModels(any()) }
        coVerify(exactly = 0) { providerModelDao.replaceModelsForProvider(any(), any(), any()) }
    }
}
