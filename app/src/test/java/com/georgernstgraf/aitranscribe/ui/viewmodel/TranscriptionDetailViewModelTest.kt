package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
import com.georgernstgraf.aitranscribe.util.ToastManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
    private lateinit var context: Context
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var securePreferences: SecurePreferences
    private lateinit var postProcessTextUseCase: PostProcessTextUseCase
    private lateinit var toastManager: ToastManager
    private lateinit var viewModel: TranscriptionDetailViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()
        clipboardManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        postProcessTextUseCase = mockk(relaxed = true)
        toastManager = mockk(relaxed = true)
        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboardManager
        coEvery { securePreferences.getLlmProvider() } returns "openrouter"
        coEvery { securePreferences.getOpenRouterApiKey() } returns "test-key"
        coEvery { securePreferences.getZaiApiKey() } returns null
        coEvery { securePreferences.getLlmModel() } returns "anthropic/claude-3-haiku"
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
        }
        Dispatchers.resetMain()
    }

    private suspend fun insertTestEntity(
        id: Long = 0,
        originalText: String = "Test transcription",
        processedText: String? = null,
        playedCount: Int = 0
    ): Long {
        return repository.insert(
            TranscriptionEntity(
                id = id,
                originalText = originalText,
                processedText = processedText,
                audioFilePath = null,
                createdAt = LocalDateTime.now().toString(),
                postProcessingType = null,
                status = "COMPLETED",
                errorMessage = null,
                playedCount = playedCount,
                retryCount = 0
            )
        )
    }

    private fun createViewModel(transcriptionId: Long, viewFilter: String = "ALL") {
        val savedStateHandle = SavedStateHandle().apply {
            set(TranscriptionDetailViewModel.KEY_TRANSCRIPTION_ID, transcriptionId)
            set(TranscriptionDetailViewModel.KEY_VIEW_FILTER, viewFilter)
        }
        viewModel = TranscriptionDetailViewModel(
            savedStateHandle,
            repository,
            securePreferences,
            postProcessTextUseCase,
            toastManager,
            context
        )
    }

    @Test
    fun `initial state loads transcription by id`() = runBlocking {
        val id = insertTestEntity(originalText = "Hello world")
        createViewModel(id)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.transcription)
        assertEquals("Hello world", state.transcription?.originalText)
    }

    @Test
    fun `initial state has no transcription for unknown id`() = runBlocking {
        createViewModel(999L)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertNull(state.transcription)
    }

    @Test
    fun `auto-marks transcription as viewed on load`() = runBlocking {
        val id = insertTestEntity(playedCount = 0)
        createViewModel(id, viewFilter = "ALL")
        testDispatcher.scheduler.runCurrent()

        val entity = repository.getById(id)
        assertTrue(entity!!.playedCount > 0)
    }

    @Test
    fun `does not auto-mark transcription as viewed when UNVIEWED_ONLY filter is active`() = runBlocking {
        val id = insertTestEntity(playedCount = 0)
        createViewModel(id, viewFilter = "UNVIEWED_ONLY")
        testDispatcher.scheduler.runCurrent()

        val entity = repository.getById(id)
        // Should remain 0 because auto-mark is suppressed for UNVIEWED_ONLY filter
        assertEquals(0, entity!!.playedCount)
    }

    @Test
    fun `toggleViewStatus viewed to unread and back to viewed`() = runBlocking {
        val id = insertTestEntity(playedCount = 1)
        createViewModel(id)
        testDispatcher.scheduler.runCurrent()

        viewModel.toggleViewStatus(id)
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.uiState.value.isViewed)

        viewModel.toggleViewStatus(id)
        testDispatcher.scheduler.runCurrent()
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value.isViewed)
    }

    @Test
    fun `deleteTranscription sets isDeleted flag and provides next id`() = runBlocking {
        val id1 = insertTestEntity(originalText = "First")
        insertTestEntity(originalText = "Second")
        createViewModel(id1)
        testDispatcher.scheduler.runCurrent()

        viewModel.deleteTranscription(id1)
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isDeleted)
        assertNotNull(state.nextTranscriptionId)
    }

    @Test
    fun `updateText updates transcription in repository`() = runBlocking {
        val id = insertTestEntity(originalText = "Old text")
        createViewModel(id)
        testDispatcher.scheduler.runCurrent()

        viewModel.updateText(id, "New text")
        testDispatcher.scheduler.runCurrent()

        val entity = repository.getById(id)
        assertEquals("New text", entity?.originalText)
    }
}
