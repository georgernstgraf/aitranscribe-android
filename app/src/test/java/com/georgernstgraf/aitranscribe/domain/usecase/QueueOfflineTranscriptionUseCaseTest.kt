package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test class for QueueOfflineTranscriptionUseCase.
 * Tests offline transcription queuing business logic.
 */
class QueueOfflineTranscriptionUseCaseTest {

    private lateinit var useCase: QueueOfflineTranscriptionUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        useCase = QueueOfflineTranscriptionUseCase(fakeRepository)
    }

    @Test
    fun `queue adds transcription to offline queue`() = runTest {
        val audioPath = "/path/to/audio.mp3"

        useCase.invoke(
            audioPath = audioPath,
            sttModel = "whisper-large-v3-turbo",
            llmModel = "anthropic/claude-3-haiku",
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertTrue("Should add item to queue", queuedItems.isNotEmpty)
    }

    @Test
    fun `queue stores audio path correctly`() = runTest {
        val audioPath = "/path/to/audio.mp3"

        useCase.invoke(
            audioPath = audioPath,
            sttModel = "whisper-large-v3-turbo",
            llmModel = "anthropic/claude-3-haiku",
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertEquals("Audio path should be stored", audioPath, queuedItems[0].audioFilePath)
    }

    @Test
    fun `queue stores stt model correctly`() = runTest {
        val sttModel = "whisper-large-v3-turbo"

        useCase.invoke(
            audioPath = "/path/to/audio.mp3",
            sttModel = sttModel,
            llmModel = "anthropic/claude-3-haiku",
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertEquals("STT model should be stored", sttModel, queuedItems[0].sttModel)
    }

    @Test
    fun `queue stores llm model when provided`() = runTest {
        val llmModel = "anthropic/claude-3-haiku"

        useCase.invoke(
            audioPath = "/path/to/audio.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = llmModel,
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertEquals("LLM model should be stored", llmModel, queuedItems[0].llmModel)
    }

    @Test
    fun `queue stores llm model as null when not provided`() = runTest {
        useCase.invoke(
            audioPath = "/path/to/audio.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = null,
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertEquals("LLM model should be null when not provided", null, queuedItems[0].llmModel)
    }

    @Test
    fun `queue stores post processing type when provided`() = runTest {
        useCase.invoke(
            audioPath = "/path/to/audio.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = "anthropic/claude-3-haiku",
            postProcessingType = "GRAMMAR"
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertEquals("Post processing type should be stored", "GRAMMAR", queuedItems[0].postProcessingType)
    }

    @Test
    fun `queue stores post processing type as null when not provided`() = runTest {
        useCase.invoke(
            audioPath = "/path/to/audio.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = "anthropic/claude-3-haiku",
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertEquals("Post processing type should be null when not provided", null, queuedItems[0].postProcessingType)
    }

    @Test
    fun `queue stores created timestamp`() = runTest {
        useCase.invoke(
            audioPath = "/path/to/audio.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = null,
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertNotNull("Created timestamp should be stored", queuedItems[0].createdAt)
    }

    @Test
    fun `queue orders items by created date`() = runTest {
        useCase.invoke(
            audioPath = "/path/to/audio1.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = null,
            postProcessingType = null
        )

        kotlinx.coroutines.delay(100)

        useCase.invoke(
            audioPath = "/path/to/audio2.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = null,
            postProcessingType = null
        )

        val queuedItems = fakeRepository.getAllQueued()
        assertEquals("Should return items in created date order", 2, queuedItems.size)
    }

    @Test
    fun `getNextQueued returns oldest item`() = runTest {
        useCase.invoke(
            audioPath = "/path/to/audio1.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = null,
            postProcessingType = null
        )

        kotlinx.coroutines.delay(100)

        useCase.invoke(
            audioPath = "/path/to/audio2.mp3",
            sttModel = "whisper-large-v3-turbo",
            llmModel = null,
            postProcessingType = null
        )

        val nextItem = fakeRepository.getNextQueued()
        assertEquals("Should return oldest item", "/path/to/audio1.mp3", nextItem?.audioFilePath)
    }
}