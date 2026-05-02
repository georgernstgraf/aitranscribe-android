package com.georgernstgraf.aitranscribe.service

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.GroqTranscriptionResponse
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.repository.Language
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepository
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
import com.georgernstgraf.aitranscribe.util.NetworkMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    private lateinit var zaiApiService: ZaiApiService
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var appSettingsStore: AppSettingsStore
    private lateinit var providerModelDao: ProviderModelDao
    private lateinit var postProcessTextUseCase: PostProcessTextUseCase
    private lateinit var languageRepository: LanguageRepository

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        
        val fakeCacheDir = File(System.getProperty("java.io.tmpdir"), "test-cache")
        fakeCacheDir.mkdirs()
        every { context.cacheDir } returns fakeCacheDir

        params = mockk(relaxed = true)
        fakeRepository = FakeTranscriptionRepository()
        groqApiService = mockk(relaxed = true)
        zaiApiService = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        appSettingsStore = mockk(relaxed = true)
        providerModelDao = mockk(relaxed = true)
        postProcessTextUseCase = mockk(relaxed = true)
        languageRepository = mockk(relaxed = true)

        every { networkMonitor.isConnected() } returns true

        coEvery { appSettingsStore.getGroqApiKey() } returns "fake-groq-key"
        coEvery { appSettingsStore.getActiveAuthToken(any()) } returns "fake-llm-token"
        coEvery { appSettingsStore.getSttProvider() } returns "groq"
        coEvery { appSettingsStore.getLlmProvider() } returns "openrouter"
        coEvery { appSettingsStore.getProviderLlmModel(any(), any()) } answers { secondArg() }
        coEvery { appSettingsStore.getProviderSttModel(any(), any()) } answers { secondArg() }

        val fakeResponse = GroqTranscriptionResponse(text = "Hello world")
        coEvery { 
            groqApiService.transcribeAudio(any(), any(), any(), any()) 
        } returns Response.success(fakeResponse)

        worker = TranscriptionWorker(
            context,
            params,
            fakeRepository,
            groqApiService,
            zaiApiService,
            networkMonitor,
            appSettingsStore,
            providerModelDao,
            postProcessTextUseCase,
            languageRepository
        )
    }

    private fun createAudioFile(): File {
        val f = File(context.cacheDir, "recording_test.m4a")
        f.writeText("fake audio content")
        return f
    }

    @Test
    fun `worker processes raw transcription and clears audio path`() = runBlocking {
        val audioFile = createAudioFile()
        val queued = TranscriptionEntity(
            id = 1,
            sttText = null,
            cleanedText = null,
            audioFilePath = audioFile.absolutePath,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            errorMessage = null,
            seen = false,
            summary = null
        )
        fakeRepository.insert(queued)

        every { params.inputData } returns Data.Builder().putLong("transcription_id", queued.id).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success().javaClass, result.javaClass)

        val saved = fakeRepository.getById(1)
        assertNotNull(saved)
        assertNull(saved!!.audioFilePath)

        coVerify(exactly = 0) {
            postProcessTextUseCase(
                transcriptionId = any(),
                isCleanupEnabled = any(),
                targetLanguage = any(),
                llmModel = any(),
                apiKey = any(),
                llmProvider = any()
            )
        }
        coVerify(exactly = 1) {
            postProcessTextUseCase.generateSummary(queued.id, any(), any(), any(), any())
        }
    }

    @Test
    fun `worker captures language from verbose_json response`() = runBlocking {
        val audioFile = createAudioFile()
        val queued = TranscriptionEntity(
            id = 0,
            sttText = null,
            cleanedText = null,
            audioFilePath = audioFile.absolutePath,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            errorMessage = null,
            seen = false,
            summary = null
        )
        val insertedId = fakeRepository.insert(queued)

        coEvery {
            groqApiService.transcribeAudio(any(), any(), any(), any())
        } returns Response.success(GroqTranscriptionResponse(text = "Hallo Welt", language = "german"))

        coEvery { languageRepository.ensureLanguageExists("de") } returns Language(id = "de", name = "German", nativeName = "Deutsch", isActive = true)

        every { params.inputData } returns Data.Builder().putLong("transcription_id", insertedId).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success().javaClass, result.javaClass)

        val saved = fakeRepository.getById(insertedId)
        assertNotNull(saved)
        assertEquals("de", saved!!.languageId)
        assertEquals("Hallo Welt", saved.sttText)
        assertNull(saved.audioFilePath)
    }
}
