package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ResetViewStatusUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: ResetViewStatusUseCase

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = ResetViewStatusUseCase(repository)
    }

    @Test
    fun `invoke resets played count to zero`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                originalText = "Test",
                processedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                postProcessingType = null,
                status = "COMPLETED",
                errorMessage = null,
                playedCount = 5,
                retryCount = 0
            )
        )

        useCase(id)

        assertEquals(0, repository.getById(id)?.playedCount)
    }
}
