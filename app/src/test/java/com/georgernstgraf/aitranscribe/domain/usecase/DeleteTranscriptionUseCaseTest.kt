package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DeleteTranscriptionUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: DeleteTranscriptionUseCase

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = DeleteTranscriptionUseCase(repository)
    }

    @Test
    fun `invoke deletes transcription by id`() = runTest {
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

        val result = useCase(mode = DeleteMode.SINGLE, transcriptionId = id)

        assertEquals(1, result)
        assertTrue(repository.getById(id) == null)
    }
}
