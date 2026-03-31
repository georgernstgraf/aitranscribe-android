package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class MarkAsViewedUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: MarkAsViewedUseCase

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = MarkAsViewedUseCase(repository)
    }

    @Test
    fun `invoke marks transcription as viewed`() = runTest {
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
                seen = false,
                retryCount = 0
            )
        )

        useCase(id)

        assertEquals(true, repository.getById(id)?.seen)
    }
}
