package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class QueueOfflineTranscriptionUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: QueueOfflineTranscriptionUseCase

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = QueueOfflineTranscriptionUseCase(repository)
    }

    @Test
    fun `invoke queues transcription for offline processing`() = runTest {
        val id = useCase(
            audioPath = "/test.mp3",
            sttModel = "whisper-large-v3",
            llmModel = "claude-3-haiku",
            postProcessingType = "CLEANUP"
        )

        assertEquals(1L, id)
        assertNotNull(repository.getQueuedById(id))
    }

    @Test(expected = QueueOfflineTranscriptionUseCase.QueueException::class)
    fun `invoke throws exception when audio path is empty`() = runTest {
        useCase(
            audioPath = "",
            sttModel = "whisper-large-v3",
            llmModel = null,
            postProcessingType = null
        )
    }

    @Test(expected = QueueOfflineTranscriptionUseCase.QueueException::class)
    fun `invoke throws exception when sttModel is empty`() = runTest {
        useCase(
            audioPath = "/test.mp3",
            sttModel = "",
            llmModel = null,
            postProcessingType = null
        )
    }
}
