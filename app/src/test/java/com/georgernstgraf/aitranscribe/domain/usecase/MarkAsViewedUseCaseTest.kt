package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MarkAsViewedUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: MarkAsViewedUseCase

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = MarkAsViewedUseCase(repository)
    }

    @Test
    fun `invoke marks transcription as viewed`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Test",
                cleanedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false
            )
        )

        useCase(id)

        assertEquals(true, repository.getById(id)?.seen)
    }
}
