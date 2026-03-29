package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModelStore
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private lateinit var viewModelStore: ViewModelStore
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()

        queuedTranscriptionDao = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        context = mockk(relaxed = true)

        coEvery { securePreferences.getSttModel() } returns "whisper-large-v3"
        coEvery { securePreferences.getLlmModel() } returns "claude-3-haiku"
        every { context.registerReceiver(any(), any()) } returns null
        coEvery { queuedTranscriptionDao.insert(any()) } returns 1L

        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has default values`() {
        viewModel = MainViewModel(repository, queuedTranscriptionDao, securePreferences, context)

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
        assertEquals(0, state.recordingDuration)
        assertNull(state.recordingError)
    }

    @Test
    fun `startRecording sets isRecording to true`() {
        viewModel = MainViewModel(repository, queuedTranscriptionDao, securePreferences, context)

        viewModel.startRecording()

        val state = viewModel.uiState.value
        assertTrue(state.isRecording)
        assertNull(state.recordingError)
    }

    @Test
    fun `stopRecording sets isRecording to false`() {
        viewModel = MainViewModel(repository, queuedTranscriptionDao, securePreferences, context)

        viewModel.startRecording()
        viewModel.stopRecording()

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
    }
}
