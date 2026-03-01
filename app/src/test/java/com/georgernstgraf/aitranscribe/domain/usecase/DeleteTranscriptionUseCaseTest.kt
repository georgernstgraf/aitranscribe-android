package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test class for DeleteTranscriptionUseCase.
 * Tests transcription deletion logic.
 */
class DeleteTranscriptionUseCaseTest {

    private lateinit var useCase: DeleteTranscriptionUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        useCase = DeleteTranscriptionUseCase(fakeRepository)
    }

    @Test
    fun `delete removes single transcription`() = runTest {
        val id = createTestTranscription()

        useCase.invoke(
            mode = com.georgernstgraf.aitranscribe.domain.model.DeleteMode.SINGLE,
            transcriptionId = id
        )

        val transcription = fakeRepository.getById(id)
        assertTrue("Transcription should be deleted", transcription == null)
    }

    @Test
    fun `delete old removes all transcriptions older than date`() = runTest {
        val now = java.time.LocalDateTime.now()
        createTestTranscription(id = 1, createdAt = now.minusDays(40))
        createTestTranscription(id = 2, createdAt = now.minusDays(20))
        createTestTranscription(id = 3, createdAt = now.minusDays(5))

        val cutoffDate = now.minusDays(30).toString()

        useCase.invoke(
            mode = com.georgernstgraf.aitranscribe.domain.model.DeleteMode.OLD_ALL,
            cutoffDate = cutoffDate,
            viewFilter = com.georgernstgraf.aitranscribe.domain.model.ViewFilter.ALL
        )

        val remaining = fakeRepository.getAllTranscriptions()
        assertEquals("Should remove transcriptions older than 30 days", 2, remaining.size)
    }

    @Test
    fun `delete old removes only unviewed transcriptions`() = runTest {
        val now = java.time.LocalDateTime.now()
        createTestTranscription(id = 1, playedCount = 1, createdAt = now.minusDays(40))
        createTestTranscription(id = 2, playedCount = 0, createdAt = now.minusDays(35))
        createTestTranscription(id = 3, playedCount = 0, createdAt = now.minusDays(25))

        val cutoffDate = now.minusDays(30).toString()

        useCase.invoke(
            mode = com.georgernstgraf.aitranscribe.domain.model.DeleteMode.OLD_UNVIEWED,
            cutoffDate = cutoffDate,
            viewFilter = com.georgernstgraf.aitranscribe.domain.model.ViewFilter.UNVIEWED_ONLY
        )

        val remaining = fakeRepository.getAllTranscriptions()
        assertEquals("Should remove only unviewed old transcriptions", 2, remaining.size)
    }

    @Test
    fun `delete old returns count of deleted items`() = runTest {
        val now = java.time.LocalDateTime.now()
        createTestTranscription(id = 1, createdAt = now.minusDays(40))
        createTestTranscription(id = 2, createdAt = now.minusDays(20))
        createTestTranscription(id = 3, createdAt = now.minusDays(5))

        val cutoffDate = now.minusDays(30).toString()

        val count = useCase.invoke(
            mode = com.georgernstgraf.aitranscribe.domain.model.DeleteMode.OLD_ALL,
            cutoffDate = cutoffDate,
            viewFilter = com.georgernstgraf.aitranscribe.domain.model.ViewFilter.ALL,
            getCount = true
        )

        assertEquals("Should return count of deleted items", 1, count)
    }

    private fun createTestTranscription(
        id: Long,
        playedCount: Int = 0,
        createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
    ): Long {
        val entity = com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity(
            id = id,
            originalText = "Test transcription",
            processedText = null,
            audioFilePath = "/path/to/audio.mp3",
            createdAt = createdAt.toString(),
            postProcessingType = null,
            status = com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
        return fakeRepository.insert(entity)
    }
}