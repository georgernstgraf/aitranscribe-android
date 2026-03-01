package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test class for ResetViewStatusUseCase.
 * Tests reset view status functionality.
 */
class ResetViewStatusUseCaseTest {

    private lateinit var useCase: ResetViewStatusUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        useCase = ResetViewStatusUseCase(fakeRepository)
    }

    @Test
    fun `reset view status sets played count to zero`() = runTest {
        val id = createTestTranscription(playedCount = 5)

        useCase.invoke(id)

        val transcription = fakeRepository.getById(id)
        assertEquals("Played count should reset to 0", 0, transcription?.playedCount)
    }

    @Test
    fun `reset view status makes transcription unviewed`() = runTest {
        val id = createTestTranscription(playedCount = 1)

        useCase.invoke(id)

        val transcription = fakeRepository.getById(id)
        assertTrue("Transcription should be unviewed", transcription?.isUnviewed == true)
    }

    @Test
    fun `reset view status can be called multiple times`() = runTest {
        val id = createTestTranscription(playedCount = 3)

        useCase.invoke(id)
        useCase.invoke(id)

        val transcription = fakeRepository.getById(id)
        assertEquals("Should remain at 0", 0, transcription?.playedCount)
    }

    @Test(expected = ResetViewStatusException::class)
    fun `reset view status throws exception for non-existent id`() = runTest {
        useCase.invoke(999)
    }

    @Test
    fun `reset view status does not change other fields`() = runTest {
        val originalText = "Test transcription"
        val id = createTestTranscription(originalText = originalText, playedCount = 1)

        useCase.invoke(id)

        val transcription = fakeRepository.getById(id)
        assertEquals("Original text should not change", originalText, transcription?.originalText)
    }

    private fun createTestTranscription(
        playedCount: Int = 0,
        originalText: String = "Test transcription"
    ): Long {
        val entity = com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity(
            id = 0,
            originalText = originalText,
            processedText = null,
            audioFilePath = "/path/to/audio.mp3",
            createdAt = java.time.LocalDateTime.now().toString(),
            postProcessingType = null,
            status = com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
        return fakeRepository.insert(entity)
    }

    class ResetViewStatusException(message: String) : Exception(message)
}