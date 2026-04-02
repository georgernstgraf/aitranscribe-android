package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SearchTranscriptionsUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: SearchTranscriptionsUseCase

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = SearchTranscriptionsUseCase(repository)
    }

    @Test
    fun `invoke returns all transcriptions with ALL filter`() = runTest {
        repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Hello world",
                cleanedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false
            )
        )

        val result = useCase(viewFilter = ViewFilter.ALL).first()

        assertEquals(1, result.size)
        assertEquals("Hello world", result[0].displayText)
    }

    @Test
    fun `invoke filters by search query in sttText`() = runTest {
        repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Hello world",
                cleanedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false
            )
        )
        repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Goodbye",
                cleanedText = null,
                audioFilePath = "/test2.mp3",
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false
            )
        )

        val result = useCase(searchQuery = "Hello", viewFilter = ViewFilter.ALL).first()

        assertEquals(1, result.size)
        assertEquals("Hello world", result[0].displayText)
    }

    @Test
    fun `invoke filters by search query in cleanedText`() = runTest {
        repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "raw hello",
                cleanedText = "Cleaned Hello world",
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false
            )
        )
        repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Goodbye",
                cleanedText = null,
                audioFilePath = "/test2.mp3",
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false
            )
        )

        val result = useCase(searchQuery = "Cleaned", viewFilter = ViewFilter.ALL).first()

        assertEquals(1, result.size)
        assertEquals("Cleaned Hello world", result[0].cleanedText)
    }
}
