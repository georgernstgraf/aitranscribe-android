package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
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
            audioPath = "/test.mp3"
        )

        assertEquals(1L, id)
        val saved = repository.getById(id)
        assertNotNull(saved)
        assertEquals(TranscriptionStatus.NO_NETWORK.name, saved?.status)
    }

    @Test(expected = QueueOfflineTranscriptionUseCase.QueueException::class)
    fun `invoke throws exception when audio path is empty`() = runTest {
        useCase(
            audioPath = ""
        )
    }
}
