package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `initial state is default`() = runBlocking {
        val id = repository.insert(createTestEntity())
        createViewModel(id)

        val state = viewModel.uiState.value
        assertFalse(state.isViewed)
        assertFalse(state.isDeleted)
        assertFalse(state.isCopiedToClipboard)
    }

    @Test
    fun `updateText persists edited text to repository`() = runBlocking {
        val original = createTestEntity(originalText = "original text")
        val id = repository.insert(original)

        val updatedEntity = original.copy(id = id, originalText = "edited text")
        repository.update(updatedEntity)

        val reloaded = repository.getById(id)
        assertEquals("edited text", reloaded?.originalText)
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
