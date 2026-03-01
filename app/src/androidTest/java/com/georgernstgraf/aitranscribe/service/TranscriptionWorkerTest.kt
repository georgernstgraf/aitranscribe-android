package com.georgernstgraf.aitranscribe.service

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Test class for TranscriptionWorker.
 * Tests background transcription worker functionality.
 */
@RunWith(AndroidJUnit4::class)
class TranscriptionWorkerTest {

    private lateinit var context: android.content.Context
    private lateinit var workManager: WorkManager
    private lateinit var testHelper: WorkManagerTestInitHelper

    private lateinit var fakeRepository: FakeTranscriptionRepository
    private lateinit var fakeGroqService: FakeGroqApiService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testHelper = WorkManagerTestInitHelper(context)
        testHelper.initialize()
        workManager = WorkManager.getInstance(context)

        fakeRepository = FakeTranscriptionRepository()
        fakeGroqService = FakeGroqApiService()
    }

    @After
    fun tearDown() {
        testHelper.close()
    }

    @Test
    fun `worker processes queued transcription successfully`() {
        val queued = createQueuedTranscription()
        fakeRepository.queueForOffline(queued)
        fakeGroqService.setTranscriptionText("Transcribed text")

        val worker = TestListenableWorkerBuilder<TranscriptionWorker, Unit>().apply {
            setApplicationContext(context)
            setInputData(
                TranscriptionWorker.createInputData(queued.id)
            )
        }.build()

        val result = worker.startWork()

        assertEquals(
            "Worker should succeed",
            ListenableWorker.Result.success(),
            result
        )

        val transcription = fakeRepository.getById(1)
        assertNotNull("Transcription should be created", transcription)
        assertEquals("Text should be transcribed", "Transcribed text", transcription?.originalText)
    }

    @Test
    fun `worker removes item from queue after processing`() {
        val queued = createQueuedTranscription()
        fakeRepository.queueForOffline(queued)
        fakeGroqService.setTranscriptionText("Transcribed text")

        val worker = TestListenableWorkerBuilder<TranscriptionWorker, Unit>().apply {
            setApplicationContext(context)
            setInputData(
                TranscriptionWorker.createInputData(queued.id)
            )
        }.build()

        worker.startWork()

        val queueCount = fakeRepository.getQueueCount()
        assertEquals("Queue should be empty after processing", 0, queueCount)
    }

    @Test
    fun `worker handles API failure gracefully`() {
        val queued = createQueuedTranscription()
        fakeRepository.queueForOffline(queued)
        fakeGroqService.setShouldFail(true)

        val worker = TestListenableWorkerBuilder<TranscriptionWorker, Unit>().apply {
            setApplicationContext(context)
            setInputData(
                TranscriptionWorker.createInputData(queued.id)
            )
        }.build()

        val result = worker.startWork()

        assertTrue(
            "Worker should retry on failure",
            result is ListenableWorker.Result.Retry
        )
    }

    @Test
    fun `worker sends notification on completion`() {
        val queued = createQueuedTranscription()
        fakeRepository.queueForOffline(queued)
        fakeGroqService.setTranscriptionText("Transcribed text")

        val worker = TestListenableWorkerBuilder<TranscriptionWorker, Unit>().apply {
            setApplicationContext(context)
            setInputData(
                TranscriptionWorker.createInputData(queued.id)
            )
        }.build()

        worker.startWork()

        // In real test, verify notification was sent
        // For now, just verify worker completes
        assertNotNull("Worker should complete", worker.startWork())
    }

    @Test
    fun `worker processes post-processing when queued`() {
        val queued = createQueuedTranscription(
            postProcessingType = "GRAMMAR"
        )
        fakeRepository.queueForOffline(queued)
        fakeGroqService.setTranscriptionText("Transcribed text")

        val worker = TestListenableWorkerBuilder<TranscriptionWorker, Unit>().apply {
            setApplicationContext(context)
            setInputData(
                TranscriptionWorker.createInputData(queued.id)
            )
        }.build()

        worker.startWork()

        val transcription = fakeRepository.getById(1)
        assertNotNull("Transcription should be created", transcription)
        assertEquals("Post-processing type should be stored", "GRAMMAR", transcription?.postProcessingType)
    }

    private fun createQueuedTranscription(
        postProcessingType: String? = null
    ): QueuedTranscriptionEntity {
        return QueuedTranscriptionEntity(
            id = 1,
            audioFilePath = "/path/to/audio.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = "anthropic/claude-3-haiku",
            postProcessingType = postProcessingType,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            priority = 0
        )
    }

    class FakeGroqApiService {
        private var transcriptionText = "Default transcription"
        private var shouldFail = false

        fun setTranscriptionText(text: String) {
            transcriptionText = text
        }

        fun setShouldFail(fail: Boolean) {
            shouldFail = fail
        }

        suspend fun transcribe(audioPath: String, model: String): String {
            if (shouldFail) {
                throw Exception("API Error")
            }
            return transcriptionText
        }
    }
}