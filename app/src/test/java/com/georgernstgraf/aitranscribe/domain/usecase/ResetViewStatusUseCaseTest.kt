package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ResetViewStatusUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: ResetViewStatusUseCase

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = ResetViewStatusUseCase(repository)
    }

    @Test
    fun `invoke resets seen to false`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Test",
                cleanedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = true
            )
        )

        useCase(id)

        assertEquals(false, repository.getById(id)?.seen)
    }
}
