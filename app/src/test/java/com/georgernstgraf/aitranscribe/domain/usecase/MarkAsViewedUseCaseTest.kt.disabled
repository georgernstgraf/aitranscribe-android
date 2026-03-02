package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test class for MarkAsViewedUseCase.
 * Tests view status management.
 */
class MarkAsViewedUseCaseTest {

    private lateinit var useCase: MarkAsViewedUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        useCase = MarkAsViewedUseCase(fakeRepository)
    }

    @Test
    fun `mark as viewed increments played count`() = runTest {
        val id = createTestTranscription(playedCount = 0)

        useCase.invoke(id)

        val transcription = fakeRepository.getById(id)
        assertEquals("Played count should increment", 1, transcription?.playedCount)
    }

    @Test
    fun `mark as viewed can be called multiple times`() = runTest {
        val id = createTestTranscription(playedCount = 0)

        useCase.invoke(id)
        useCase.invoke(id)
        useCase.invoke(id)

        val transcription = fakeRepository.getById(id)
        assertEquals("Should increment on each call", 3, transcription?.playedCount)
    }

    @Test(expected = MarkAsViewedException::class)
    fun `mark as viewed throws exception for non-existent id`() = runTest {
        useCase.invoke(999)
    }

    @Test
    fun `mark as viewed does not change other fields`() = runTest {
        val originalText = "Test transcription"
        val id = createTestTranscription(originalText = originalText)

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

    class MarkAsViewedException(message: String) : Exception(message)
}