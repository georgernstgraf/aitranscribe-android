package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.util.NetworkMonitor
import com.georgernstgraf.aitranscribe.util.ToastManager
import io.mockk.coEvery
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
    private lateinit var appSettingsStore: AppSettingsStore
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

        appSettingsStore = mockk(relaxed = true)
        toastManager = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        context = mockk(relaxed = true)

        coEvery { appSettingsStore.getProviderSttModel(any(), any()) } returns "whisper-large-v3"
        coEvery { appSettingsStore.getProviderLlmModel(any(), any()) } returns "claude-3-haiku"
        coEvery { appSettingsStore.getSttProvider() } returns "groq"
        coEvery { appSettingsStore.getLlmProvider() } returns "openrouter"
        coEvery { appSettingsStore.getActiveAuthToken("groq") } returns "test-key"
        every { context.registerReceiver(any(), any()) } returns null
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
        return MainViewModel(repository, appSettingsStore, toastManager, networkMonitor, context)
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
        repository.insert(TranscriptionEntity(originalText = "", processedText = null, audioFilePath = "/a.m4a", createdAt = LocalDateTime.now().toString(), status = TranscriptionStatus.NO_NETWORK.name, errorMessage = null))
        repository.insert(TranscriptionEntity(originalText = "", processedText = null, audioFilePath = "/b.m4a", createdAt = LocalDateTime.now().toString(), status = TranscriptionStatus.STT_ERROR_RETRYABLE.name, errorMessage = null))
        coEvery { appSettingsStore.getProviderSttModel(any(), any()) } returns "whisper-large-v3-turbo"

        val workManager = mockk<androidx.work.WorkManager>(relaxed = true)
        mockkStatic(androidx.work.WorkManager::class)
        every { androidx.work.WorkManager.getInstance(any()) } returns workManager

        every { networkMonitor.isConnected() } returns false
        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        networkStateFlow.value = true
        testDispatcher.scheduler.runCurrent()

        assertEquals(TranscriptionStatus.PENDING.name, repository.getById(1)?.status)
        assertEquals(TranscriptionStatus.PENDING.name, repository.getById(2)?.status)
    }

    @Test
    fun `network reconnect does not retry when queue is empty`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        networkStateFlow.value = true
        testDispatcher.scheduler.runCurrent()
        assertEquals(null, repository.getById(1))
    }

    @Test
    fun `network disconnect does not trigger retry`() = runBlocking {
        val queuedItem = TranscriptionEntity(id = 1, originalText = "", processedText = null, audioFilePath = "/a.m4a", createdAt = LocalDateTime.now().toString(), status = TranscriptionStatus.NO_NETWORK.name, errorMessage = null)
        repository.insert(queuedItem)

        every { networkMonitor.isConnected() } returns false
        networkStateFlow.value = false
        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        networkStateFlow.value = false
        testDispatcher.scheduler.runCurrent()

        assertEquals(TranscriptionStatus.NO_NETWORK.name, repository.getById(1)?.status)
    }

    @Test
    fun `app start while online retries pending transcriptions`() = runBlocking {
        repository.insert(
            TranscriptionEntity(
                originalText = "",
                processedText = null,
                audioFilePath = "/c.m4a",
                createdAt = LocalDateTime.now().toString(),
                status = TranscriptionStatus.PENDING.name,
                errorMessage = null
            )
        )
        coEvery { appSettingsStore.getProviderSttModel(any(), any()) } returns "whisper-large-v3-turbo"

        val workManager = mockk<androidx.work.WorkManager>(relaxed = true)
        mockkStatic(androidx.work.WorkManager::class)
        every { androidx.work.WorkManager.getInstance(any()) } returns workManager

        every { networkMonitor.isConnected() } returns true
        networkStateFlow.value = true

        viewModel = createViewModel()
        testDispatcher.scheduler.runCurrent()

        assertEquals(TranscriptionStatus.PENDING.name, repository.getById(1)?.status)
    }
}
