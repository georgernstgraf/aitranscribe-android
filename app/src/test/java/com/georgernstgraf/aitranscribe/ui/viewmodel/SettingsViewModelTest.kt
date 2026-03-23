package com.georgernstgraf.aitranscribe.ui.viewmodel

import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.DeleteTranscriptionUseCase
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var deleteUseCase: DeleteTranscriptionUseCase
    private lateinit var securePreferences: SecurePreferences
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()
        deleteUseCase = DeleteTranscriptionUseCase(repository)
        securePreferences = mockk(relaxed = true)
        
        coEvery { securePreferences.getGroqApiKey() } returns null
        coEvery { securePreferences.getOpenRouterApiKey() } returns null
        coEvery { securePreferences.getSttModel() } returns "whisper-large-v3-turbo"
        coEvery { securePreferences.getLlmModel() } returns "anthropic/claude-3-haiku"
        
        viewModel = SettingsViewModel(deleteUseCase, securePreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads settings from preferences`() = runTest {
        val state = viewModel.uiState.first()
        
        assertEquals("whisper-large-v3-turbo", state.sttModel)
        assertEquals("anthropic/claude-3-haiku", state.llmModel)
    }

    @Test
    fun `onSttModelChanged updates state`() = runTest {
        viewModel.onSttModelChanged("whisper-large-v3")
        
        val state = viewModel.uiState.first()
        assertEquals("whisper-large-v3", state.sttModel)
    }

    @Test
    fun `onLlmModelChanged updates state`() = runTest {
        viewModel.onLlmModelChanged("gpt-4")
        
        val state = viewModel.uiState.first()
        assertEquals("gpt-4", state.llmModel)
    }

    @Test
    fun `onGroqApiKeyChanged updates state`() = runTest {
        viewModel.onGroqApiKeyChanged("test-key")
        
        val state = viewModel.uiState.first()
        assertEquals("test-key", state.groqApiKey)
    }

    @Test
    fun `onOpenRouterApiKeyChanged updates state`() = runTest {
        viewModel.onOpenRouterApiKeyChanged("or-key")
        
        val state = viewModel.uiState.first()
        assertEquals("or-key", state.openRouterApiKey)
    }

    @Test
    fun `onDaysToDeleteChanged updates state`() = runTest {
        viewModel.onDaysToDeleteChanged(60)
        
        val state = viewModel.uiState.first()
        assertEquals(60, state.daysToDelete)
    }

    @Test
    fun `onDeleteViewFilterChanged updates state`() = runTest {
        viewModel.onDeleteViewFilterChanged(ViewFilter.ALL)
        
        val state = viewModel.uiState.first()
        assertEquals(ViewFilter.ALL, state.deleteViewFilter)
    }
}
