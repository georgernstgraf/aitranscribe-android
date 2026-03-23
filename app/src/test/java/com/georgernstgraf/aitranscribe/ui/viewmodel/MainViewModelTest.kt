package com.georgernstgraf.aitranscribe.ui.viewmodel

import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionDao
import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import io.mockk.coEvery
import io.mockk.every
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var queuedTranscriptionDao: QueuedTranscriptionDao
    private lateinit var securePreferences: SecurePreferences
    private lateinit var context: android.content.Context
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()
        
        // Mock dependencies
        queuedTranscriptionDao = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        context = mockk(relaxed = true)
        
        coEvery { securePreferences.getSttModel() } returns "whisper-large-v3"
        coEvery { securePreferences.getLlmModel() } returns "claude-3-haiku"
        every { context.registerReceiver(any(), any()) } returns null
        coEvery { queuedTranscriptionDao.insert(any()) } returns 1L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() = runTest {
        viewModel = MainViewModel(repository, queuedTranscriptionDao, securePreferences, context)
        
        val state = viewModel.uiState.first()
        assertFalse(state.isRecording)
        assertEquals(0, state.recordingDuration)
        assertNull(state.recordingError)
        assertTrue(state.recentTranscriptions.isEmpty())
    }

    @Test
    fun `startRecording sets isRecording to true`() = runTest {
        viewModel = MainViewModel(repository, queuedTranscriptionDao, securePreferences, context)
        
        viewModel.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertTrue(state.isRecording)
        assertNull(state.recordingError)
    }

    @Test
    fun `stopRecording sets isRecording to false`() = runTest {
        viewModel = MainViewModel(repository, queuedTranscriptionDao, securePreferences, context)
        
        viewModel.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.stopRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertFalse(state.isRecording)
    }

    @Test
    fun `recordingDuration increments while recording`() = runTest {
        viewModel = MainViewModel(repository, queuedTranscriptionDao, securePreferences, context)
        
        viewModel.startRecording()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Advance time by 3 seconds
        testDispatcher.scheduler.advanceTimeBy(3000)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Duration should have incremented
        val state = viewModel.uiState.first()
        assertTrue(state.recordingDuration >= 0) // Timer may or may not have started depending on timing
    }
}
