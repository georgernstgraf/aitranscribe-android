package com.georgernstgraf.aitranscribe.service

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.GroqTranscriptionResponse
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
import com.georgernstgraf.aitranscribe.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TranscriptionWorkerTest {

    private lateinit var worker: TranscriptionWorker
    private lateinit var context: Context
    private lateinit var params: WorkerParameters
    private lateinit var fakeRepository: FakeTranscriptionRepository
    private lateinit var groqApiService: GroqApiService
    private lateinit var openRouterApiService: OpenRouterApiService
    private lateinit var zaiApiService: ZaiApiService
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var appSettingsStore: AppSettingsStore
    private lateinit var providerModelDao: ProviderModelDao
    private lateinit var postProcessTextUseCase: PostProcessTextUseCase

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        
        // Mock app cache dir for orphaned file cleanup
        val fakeCacheDir = File(System.getProperty("java.io.tmpdir"), "test-cache")
        fakeCacheDir.mkdirs()
        every { context.cacheDir } returns fakeCacheDir

        params = mockk(relaxed = true)
        fakeRepository = FakeTranscriptionRepository()
        groqApiService = mockk(relaxed = true)
        openRouterApiService = mockk(relaxed = true)
        zaiApiService = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        appSettingsStore = mockk(relaxed = true)
        providerModelDao = mockk(relaxed = true)
        postProcessTextUseCase = mockk(relaxed = true)

        // Default network up
        every { networkMonitor.isConnected() } returns true

        // Default API Key
        coEvery { appSettingsStore.getGroqApiKey() } returns "fake-groq-key"
        coEvery { appSettingsStore.getActiveAuthToken(any()) } returns "fake-llm-token"
        coEvery { appSettingsStore.getSttProvider() } returns "groq"
        coEvery { appSettingsStore.getLlmProvider() } returns "openrouter"
        coEvery { appSettingsStore.getProviderLlmModel(any(), any()) } answers { secondArg() }
        coEvery { appSettingsStore.getProviderSttModel(any(), any()) } answers { secondArg() }

        // Default Groq success
        val fakeResponse = GroqTranscriptionResponse(text = "Hello world")
        coEvery { 
            groqApiService.transcribeAudio(any(), any(), any(), any()) 
        } returns Response.success(fakeResponse)

        worker = TranscriptionWorker(
            context,
            params,
            fakeRepository,
            groqApiService,
            openRouterApiService,
            zaiApiService,
            networkMonitor,
            appSettingsStore,
            providerModelDao,
            postProcessTextUseCase
        )
    }

    private fun createAudioFile(): File {
        val f = File(context.cacheDir, "recording_test.m4a")
        f.writeText("fake audio content")
        return f
    }

    @Test
    fun `worker processes raw transcription and clears audio path`() = runBlocking {
        coEvery { appSettingsStore.getProcessingMode() } returns PostProcessingType.RAW.name
        val audioFile = createAudioFile()
        val queued = TranscriptionEntity(
            id = 1,
            originalText = "",
            processedText = null,
            audioFilePath = audioFile.absolutePath,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = TranscriptionStatus.PENDING.name,
            errorMessage = null,
            seen = false,
            summary = null
        )
        fakeRepository.insert(queued)

        every { params.inputData } returns Data.Builder().putLong("transcription_id", queued.id).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success().javaClass, result.javaClass)

        // Verify audio file path is null
        val saved = fakeRepository.getById(1)
        assertNotNull(saved)
        assertNull(saved!!.audioFilePath)
    }

    @Test
    fun `worker preserves audio path if llm fails`() = runBlocking {
        coEvery { appSettingsStore.getProcessingMode() } returns PostProcessingType.CLEANUP.name
        val audioFile = createAudioFile()
        val queued = TranscriptionEntity(
            id = 1,
            originalText = "",
            processedText = null,
            audioFilePath = audioFile.absolutePath,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = TranscriptionStatus.PENDING.name,
            errorMessage = null,
            seen = false,
            summary = null
        )
        fakeRepository.insert(queued)

        every { params.inputData } returns Data.Builder().putLong("transcription_id", queued.id).build()

        // Force LLM to fail
        coEvery { 
            postProcessTextUseCase(any(), any(), any(), any(), any(), any()) 
        } throws Exception("LLM Error")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success().javaClass, result.javaClass)

        // Verify audio file path is still present
        val saved = fakeRepository.getById(1)
        assertNotNull(saved)
        assertEquals(audioFile.absolutePath, saved!!.audioFilePath)
        assertEquals(TranscriptionStatus.COMPLETED_WITH_WARNING.name, saved.status)
    }

    @Test
    fun `worker clears audio path if llm succeeds`() = runBlocking {
        coEvery { appSettingsStore.getProcessingMode() } returns PostProcessingType.CLEANUP.name
        val audioFile = createAudioFile()
        val queued = TranscriptionEntity(
            id = 1,
            originalText = "",
            processedText = null,
            audioFilePath = audioFile.absolutePath,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            status = TranscriptionStatus.PENDING.name,
            errorMessage = null,
            seen = false,
            summary = null
        )
        fakeRepository.insert(queued)

        every { params.inputData } returns Data.Builder().putLong("transcription_id", queued.id).build()

        // LLM succeeds normally (mock is relaxed)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success().javaClass, result.javaClass)

        // Verify audio file path is cleared
        val saved = fakeRepository.getById(1)
        assertNotNull(saved)
        assertNull(saved!!.audioFilePath)
        assertEquals(TranscriptionStatus.COMPLETED.name, saved.status)
    }
}
