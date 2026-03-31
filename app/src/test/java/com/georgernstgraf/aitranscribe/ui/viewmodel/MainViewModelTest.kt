package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionDao
import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.util.NetworkMonitor
import com.georgernstgraf.aitranscribe.util.ToastManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
    private lateinit var toastManager: ToastManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var context: android.content.Context
    private lateinit var viewModel: MainViewModel
    private lateinit var viewModelStore: ViewModelStore
    private val testDispatcher = StandardTestDispatcher()
    private val networkStateFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()

        queuedTranscriptionDao = mockk(relaxed = true)
        securePreferences = mockk(relaxed = true)
        toastManager = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        context = mockk(relaxed = true)

        coEvery { securePreferences.getSttModel() } returns "whisper-large-v3"
        coEvery { securePreferences.getLlmModel() } returns "claude-3-haiku"
        coEvery { securePreferences.getSttProvider() } returns "groq"
        coEvery { securePreferences.getActiveAuthToken("groq") } returns "test-key"
        every { context.registerReceiver(any(), any()) } returns null
        coEvery { queuedTranscriptionDao.insert(any()) } returns 1L
        every { networkMonitor.isConnected() } returns false
        every { networkMonitor.networkState } returns networkStateFlow

        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(repository, queuedTranscriptionDao, securePreferences, toastManager, networkMonitor, context)
    }

    @Test
    fun `initial state has default values`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
        assertEquals(0, state.recordingDuration)
        assertNull(state.recordingError)
    }

    @Test
    fun `startRecording sets isRecording to true`() = runBlocking {
        viewModel = createViewModel()

        viewModel.startRecording()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isRecording)
        assertNull(state.recordingError)
    }

    @Test
    fun `stopRecording sets isRecording to false`() = runBlocking {
        viewModel = createViewModel()

        viewModel.startRecording()
        testDispatcher.scheduler.runCurrent()
        viewModel.stopRecording()
        testDispatcher.scheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isRecording)
    }

    @Test
    fun `network reconnect retries queued transcriptions with updated model`() = runBlocking {
        val queuedItems = listOf(
            QueuedTranscriptionEntity(id = 1, audioFilePath = "/a.m4a", sttModel = "old-model", llmModel = "llm", postProcessingType = "RAW", createdAt = LocalDateTime.now().toString(), priority = 0),
            QueuedTranscriptionEntity(id = 2, audioFilePath = "/b.m4a", sttModel = "old-model", llmModel = "llm", postProcessingType = "RAW", createdAt = LocalDateTime.now().toString(), priority = 0)
        )
        coEvery { queuedTranscriptionDao.getAllSync() } returns queuedItems
        coEvery { securePreferences.getSttModel() } returns "whisper-large-v3-turbo"

        val workManager = mockk<androidx.work.WorkManager>(relaxed = true)
        mockkStatic(androidx.work.WorkManager::class)
        every { androidx.work.WorkManager.getInstance(any()) } returns workManager

        every { networkMonitor.isConnected() } returns false
        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        networkStateFlow.value = true
        testDispatcher.scheduler.runCurrent()

        coVerify { queuedTranscriptionDao.updateSttModel(1, "whisper-large-v3-turbo") }
        coVerify { queuedTranscriptionDao.updateSttModel(2, "whisper-large-v3-turbo") }
    }

    @Test
    fun `network reconnect does not retry when queue is empty`() = runBlocking {
        coEvery { queuedTranscriptionDao.getAllSync() } returns emptyList()

        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        networkStateFlow.value = true
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { queuedTranscriptionDao.updateSttModel(any(), any()) }
    }

    @Test
    fun `network disconnect does not trigger retry`() = runBlocking {
        val queuedItems = listOf(
            QueuedTranscriptionEntity(id = 1, audioFilePath = "/a.m4a", sttModel = "old-model", llmModel = "llm", postProcessingType = "RAW", createdAt = LocalDateTime.now().toString(), priority = 0)
        )
        coEvery { queuedTranscriptionDao.getAllSync() } returns queuedItems

        every { networkMonitor.isConnected() } returns true
        networkStateFlow.value = true
        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        networkStateFlow.value = false
        testDispatcher.scheduler.runCurrent()

        coVerify(exactly = 0) { queuedTranscriptionDao.updateSttModel(any(), any()) }
    }
}
