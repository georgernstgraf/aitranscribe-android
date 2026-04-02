package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class QueueOfflineTranscriptionUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: QueueOfflineTranscriptionUseCase

    @BeforeEach
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
        assertEquals("/test.mp3", saved?.audioFilePath)
        assertEquals(null, saved?.sttText)
        assertEquals("No network available", saved?.errorMessage)
    }

    @Test
    fun `invoke throws exception when audio path is empty`() {
        assertThrows(QueueOfflineTranscriptionUseCase.QueueException::class.java) {
            runTest { useCase(audioPath = "") }
        }
    }
}
