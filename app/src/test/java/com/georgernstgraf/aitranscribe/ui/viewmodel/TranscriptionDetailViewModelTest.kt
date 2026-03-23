package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptionDetailViewModelTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: TranscriptionDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(transcriptionId: Long) {
        savedStateHandle = SavedStateHandle(mapOf("transcription_id" to transcriptionId))
        viewModel = TranscriptionDetailViewModel(
            savedStateHandle = savedStateHandle,
            repository = repository,
            context = mockk(relaxed = true)
        )
    }

    @Test
    fun `initial state is empty`() = runTest {
        val id = repository.insert(createTestEntity())
        createViewModel(id)

        val state = viewModel.uiState.first()
        assertNull(state.transcription)
        assertFalse(state.isViewed)
        assertFalse(state.isDeleted)
        assertFalse(state.isCopiedToClipboard)
    }

    @Test
    fun `loadTranscription loads transcription from repository`() = runTest {
        val id = repository.insert(createTestEntity(originalText = "Test transcription"))
        createViewModel(id)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertNotNull(state.transcription)
        assertEquals("Test transcription", state.transcription?.originalText)
    }

    @Test
    fun `deleteTranscription marks isDeleted true`() = runTest {
        val id = repository.insert(createTestEntity())
        createViewModel(id)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteTranscription(id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state.isDeleted)
        assertNull(repository.getById(id))
    }

    @Test
    fun `resetViewStatus updates isViewed to false`() = runTest {
        val id = repository.insert(createTestEntity(playedCount = 5))
        createViewModel(id)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetViewStatus(id)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertFalse(state.isViewed)
        assertEquals(0, repository.getById(id)?.playedCount)
    }

    private fun createTestEntity(
        originalText: String = "Test",
        playedCount: Int = 0
    ): TranscriptionEntity {
        return TranscriptionEntity(
            id = 0,
            originalText = originalText,
            processedText = null,
            audioFilePath = "/test.mp3",
            createdAt = LocalDateTime.now().toString(),
            postProcessingType = null,
            status = "COMPLETED",
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
    }
}
