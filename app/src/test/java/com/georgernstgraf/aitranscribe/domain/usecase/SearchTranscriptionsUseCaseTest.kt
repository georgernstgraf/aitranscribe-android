package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test class for SearchTranscriptionsUseCase.
 * Tests search and filter business logic.
 */
class SearchTranscriptionsUseCaseTest {

    private lateinit var useCase: SearchTranscriptionsUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        useCase = SearchTranscriptionsUseCase(fakeRepository)
    }

    @Test
    fun `search returns all transcriptions with ALL filter`() = runTest {
        createTestTranscriptions()

        val results = useCase.invoke(
            startDate = null,
            endDate = null,
            searchQuery = null,
            viewFilter = ViewFilter.ALL
        )

        assertTrue("Should return all transcriptions", results.size >= 3)
    }

    @Test
    fun `search returns only unviewed transcriptions with UNVIEWED filter`() = runTest {
        val viewedTranscription = createTestTranscription(
            id = 1,
            playedCount = 1
        )
        val unviewedTranscription = createTestTranscription(
            id = 2,
            playedCount = 0
        )

        val results = useCase.invoke(
            startDate = null,
            endDate = null,
            searchQuery = null,
            viewFilter = ViewFilter.UNVIEWED_ONLY
        )

        assertEquals("Should return only unviewed", 1, results.size)
        assertEquals("Should be unviewed transcription", unviewedTranscription.id, results[0].id)
    }

    @Test
    fun `search filters by date range`() = runTest {
        val now = LocalDateTime.now()
        createTestTranscription(id = 1, createdAt = now.minusDays(2))
        createTestTranscription(id = 2, createdAt = now.minusDays(1))
        createTestTranscription(id = 3, createdAt = now)

        val startDate = now.minusDays(1).toString()
        val endDate = now.toString()

        val results = useCase.invoke(
            startDate = startDate,
            endDate = endDate,
            searchQuery = null,
            viewFilter = ViewFilter.ALL
        )

        assertTrue("Should filter by date range", results.size >= 2)
    }

    @Test
    fun `search filters by text query`() = runTest {
        createTestTranscription(id = 1, originalText = "Meeting notes")
        createTestTranscription(id = 2, originalText = "Quick memo")
        createTestTranscription(id = 3, originalText = "Meeting about project")

        val results = useCase.invoke(
            startDate = null,
            endDate = null,
            searchQuery = "Meeting",
            viewFilter = ViewFilter.ALL
        )

        assertEquals("Should filter by text", 2, results.size)
    }

    @Test
    fun `search is case insensitive`() = runTest {
        createTestTranscription(id = 1, originalText = "Test transcription")

        val results = useCase.invoke(
            startDate = null,
            endDate = null,
            searchQuery = "TEST",
            viewFilter = ViewFilter.ALL
        )

        assertEquals("Should be case insensitive", 1, results.size)
    }

    @Test
    fun `search combines multiple filters`() = runTest {
        val now = LocalDateTime.now()
        createTestTranscription(id = 1, originalText = "Meeting notes", playedCount = 0, createdAt = now.minusDays(2))
        createTestTranscription(id = 2, originalText = "Quick memo", playedCount = 0, createdAt = now.minusDays(1))
        createTestTranscription(id = 3, originalText = "Meeting about project", playedCount = 1, createdAt = now)

        val startDate = now.minusDays(1).toString()

        val results = useCase.invoke(
            startDate = startDate,
            endDate = null,
            searchQuery = "memo",
            viewFilter = ViewFilter.UNVIEWED_ONLY
        )

        assertEquals("Should combine all filters", 1, results.size)
    }

    @Test
    fun `search orders by date descending`() = runTest {
        val now = LocalDateTime.now()
        createTestTranscription(id = 1, createdAt = now.minusDays(2))
        createTestTranscription(id = 2, createdAt = now.minusDays(1))
        createTestTranscription(id = 3, createdAt = now)

        val results = useCase.invoke(
            startDate = null,
            endDate = null,
            searchQuery = null,
            viewFilter = ViewFilter.ALL
        )

        assertTrue("Should be ordered by date descending", 
            results[0].createdAt.isAfter(results[1].createdAt))
    }

    private fun createTestTranscriptions() {
        createTestTranscription(id = 1, playedCount = 0)
        createTestTranscription(id = 2, playedCount = 0)
        createTestTranscription(id = 3, playedCount = 1)
    }

    private fun createTestTranscription(
        id: Long,
        originalText: String = "Test transcription",
        playedCount: Int = 0,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Transcription {
        val entity = com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity(
            id = id,
            originalText = originalText,
            processedText = null,
            audioFilePath = "/path/to/audio.mp3",
            createdAt = createdAt.toString(),
            postProcessingType = null,
            status = TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
        fakeRepository.insert(entity)
        return entity.toDomain()
    }
}