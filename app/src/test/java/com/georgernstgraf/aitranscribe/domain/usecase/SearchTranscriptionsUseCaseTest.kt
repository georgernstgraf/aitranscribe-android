package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class SearchTranscriptionsUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: SearchTranscriptionsUseCase

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = SearchTranscriptionsUseCase(repository)
    }

    @Test
    fun `invoke returns all transcriptions with ALL filter`() = runTest {
        repository.insert(
            TranscriptionEntity(
                id = 0,
                originalText = "Hello world",
                processedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                status = "COMPLETED",
                errorMessage = null,
                seen = false
            )
        )

        val result = useCase(viewFilter = ViewFilter.ALL).first()

        assertEquals(1, result.size)
        assertEquals("Hello world", result[0].originalText)
    }

    @Test
    fun `invoke filters by search query`() = runTest {
        repository.insert(
            TranscriptionEntity(
                id = 0,
                originalText = "Hello world",
                processedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                status = "COMPLETED",
                errorMessage = null,
                seen = false
            )
        )
        repository.insert(
            TranscriptionEntity(
                id = 0,
                originalText = "Goodbye",
                processedText = null,
                audioFilePath = "/test2.mp3",
                createdAt = LocalDateTime.now().toString(),
                status = "COMPLETED",
                errorMessage = null,
                seen = false
            )
        )

        val result = useCase(searchQuery = "Hello", viewFilter = ViewFilter.ALL).first()

        assertEquals(1, result.size)
        assertEquals("Hello world", result[0].originalText)
    }
}
