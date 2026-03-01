package com.georgernstgraf.aitranscribe.ui.viewmodel

import app.cash.turbine.test
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test class for TranscriptionDetailViewModel.
 * Tests transcription detail UI logic.
 */
class TranscriptionDetailViewModelTest {

    private lateinit var viewModel: TranscriptionDetailViewModel
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        viewModel = TranscriptionDetailViewModel(fakeRepository)
    }

    @Test
    fun `loadTranscription emits transcription`() = runTest {
        val transcriptionId = createTestTranscription()

        viewModel.loadTranscription(transcriptionId)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull("Should emit transcription", state.transcription)
            assertEquals("Should match ID", transcriptionId, state.transcription?.id)
        }
    }

    @Test
    fun `loadTranscription marks as viewed`() = runTest {
        val transcriptionId = createTestTranscription(playedCount = 0)

        viewModel.loadTranscription(transcriptionId)

        viewModel.uiState.test {
            awaitItem()
            
            val transcription = fakeRepository.getById(transcriptionId)
            assertTrue("Should mark as viewed", transcription?.playedCount == 1)
        }
    }

    @Test
    fun `loadTranscription updates viewed state`() = runTest {
        val transcriptionId = createTestTranscription(playedCount = 0)

        viewModel.loadTranscription(transcriptionId)

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse("Should update viewed state", state.isViewed)
        }
    }

    @Test
    fun `resetViewStatus marks transcription as unviewed`() = runTest {
        val transcriptionId = createTestTranscription(playedCount = 1)

        viewModel.resetViewStatus(transcriptionId)

        val transcription = fakeRepository.getById(transcriptionId)
        assertEquals("Should reset played count", 0, transcription?.playedCount)
    }

    @Test
    fun `deleteTranscription removes transcription`() = runTest {
        val transcriptionId = createTestTranscription()

        viewModel.deleteTranscription(transcriptionId)

        val transcription = fakeRepository.getById(transcriptionId)
        assertTrue("Should remove transcription", transcription == null)
    }

    @Test
    fun `deleteTranscription emits deletion state`() = runTest {
        val transcriptionId = createTestTranscription()

        viewModel.deleteTranscription(transcriptionId)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Should emit deleted state", state.isDeleted)
        }
    }

    @Test
    fun `copyToClipboard copies text`() = runTest {
        val transcription = createTestTranscription(
            originalText = "Original text",
            processedText = "Processed text"
        )

        viewModel.loadTranscription(transcription)

        viewModel.uiState.test {
            awaitItem()
            
            viewModel.copyToClipboard()
            
            val state = awaitItem()
            assertTrue("Should emit copied state", state.isCopiedToClipboard)
        }
    }

    private fun createTestTranscription(
        playedCount: Int = 0
    ): Long {
        val entity = com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity(
            id = 1,
            originalText = "Test transcription",
            processedText = "Processed text",
            audioFilePath = "/path/to/audio.mp3",
            createdAt = LocalDateTime.now().toString(),
            postProcessingType = null,
            status = com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
        return fakeRepository.insert(entity)
    }
}