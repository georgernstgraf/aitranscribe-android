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
                text = "Test",
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                status = "COMPLETED",
                errorMessage = null,
                seen = true
            )
        )

        useCase(id)

        assertEquals(false, repository.getById(id)?.seen)
    }
}
