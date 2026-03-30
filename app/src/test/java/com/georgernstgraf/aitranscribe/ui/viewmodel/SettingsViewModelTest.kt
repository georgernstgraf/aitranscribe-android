package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.DeleteTranscriptionUseCase
import com.georgernstgraf.aitranscribe.domain.usecase.ValidateApiKeysUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var deleteUseCase: DeleteTranscriptionUseCase
    private lateinit var validateApiKeysUseCase: ValidateApiKeysUseCase
    private lateinit var securePreferences: SecurePreferences
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()
        deleteUseCase = DeleteTranscriptionUseCase(repository)
        securePreferences = mockk(relaxed = true)
        validateApiKeysUseCase = ValidateApiKeysUseCase(OkHttpClient())

        coEvery { securePreferences.getProviderApiKey("groq") } returns null
        coEvery { securePreferences.getProviderApiKey("openrouter") } returns null
        coEvery { securePreferences.getProviderApiKey("zai") } returns null
        coEvery { securePreferences.getProviderModel("groq", any()) } returns "whisper-large-v3-turbo"
        coEvery { securePreferences.getProviderModel("openrouter", any()) } returns "anthropic/claude-3-haiku"
        coEvery { securePreferences.getProviderModel("zai", any()) } returns "glm-4.7"
        coEvery { securePreferences.getLlmProvider() } returns "openrouter"
        coEvery { securePreferences.getSttProvider() } returns "groq"
        coEvery { securePreferences.getGroqApiKey() } returns null

        viewModel = SettingsViewModel(deleteUseCase, securePreferences, validateApiKeysUseCase)
    }

    @Test
    fun `initial state loads settings from preferences`() = runBlocking {
        testDispatcher.scheduler.runCurrent()
        val state = viewModel.uiState.value

        assertEquals("whisper-large-v3-turbo", state.sttModel)
        assertEquals("anthropic/claude-3-haiku", state.llmModel)
    }


    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `onSttModelChanged updates state`() = runBlocking {
        viewModel.onSttModelChanged("whisper-large-v3")

        val state = viewModel.uiState.value
        assertEquals("whisper-large-v3", state.sttModel)
    }

    @Test
    fun `onLlmModelChanged updates state`() = runBlocking {
        viewModel.onLlmModelChanged("gpt-4")

        val state = viewModel.uiState.value
        assertEquals("gpt-4", state.llmModel)
    }

    @Test
    fun `onGroqApiKeyChanged updates state`() = runBlocking {
        viewModel.onGroqApiKeyChanged("test-key")

        val state = viewModel.uiState.value
        assertEquals("test-key", state.groqApiKey)
    }

    @Test
    fun `onOpenRouterApiKeyChanged updates state`() = runBlocking {
        viewModel.onOpenRouterApiKeyChanged("or-key")

        val state = viewModel.uiState.value
        assertEquals("or-key", state.openRouterApiKey)
    }

    @Test
    fun `onDaysToDeleteChanged updates state`() = runBlocking {
        viewModel.onDaysToDeleteChanged(60)

        val state = viewModel.uiState.value
        assertEquals(60, state.daysToDelete)
    }

    @Test
    fun `onDeleteViewFilterChanged updates state`() = runBlocking {
        viewModel.onDeleteViewFilterChanged(ViewFilter.ALL)

        val state = viewModel.uiState.value
        assertEquals(ViewFilter.ALL, state.deleteViewFilter)
    }
}
