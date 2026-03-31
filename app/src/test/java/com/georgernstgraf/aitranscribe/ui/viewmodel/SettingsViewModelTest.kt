package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.ModelEntity
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
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
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var deleteUseCase: DeleteTranscriptionUseCase
    private lateinit var validateApiKeysUseCase: ValidateApiKeysUseCase
    private lateinit var appSettingsStore: AppSettingsStore
    private lateinit var providerModelDao: ProviderModelDao
    private lateinit var context: android.content.Context
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeTranscriptionRepository()
        deleteUseCase = DeleteTranscriptionUseCase(repository)
        appSettingsStore = mockk(relaxed = true)
        providerModelDao = mockk(relaxed = true)
        context = mockk(relaxed = true)
        validateApiKeysUseCase = ValidateApiKeysUseCase(OkHttpClient())

        coEvery { providerModelDao.getModelsForProvider(any()) } returns emptyList()

        coEvery { appSettingsStore.getActiveAuthToken("groq") } returns null
        coEvery { appSettingsStore.getActiveAuthToken("openrouter") } returns null
        coEvery { appSettingsStore.getActiveAuthToken("zai") } returns null
        coEvery { appSettingsStore.getProviderSttModel("groq", any()) } returns "whisper-large-v3-turbo"
        coEvery { appSettingsStore.getProviderLlmModel("openrouter", any()) } returns "anthropic/claude-3-haiku"
        coEvery { appSettingsStore.getProviderLlmModel("zai", any()) } returns "glm-4.7"
        coEvery { appSettingsStore.getLlmProvider() } returns "openrouter"
        coEvery { appSettingsStore.getSttProvider() } returns "groq"
        viewModel = SettingsViewModel(deleteUseCase, repository, appSettingsStore, validateApiKeysUseCase, providerModelDao, context)
    }

    @Test
    fun `initial state loads settings from preferences`() = runBlocking {
        testDispatcher.scheduler.runCurrent()
        val state = viewModel.uiState.value

        assertEquals("whisper-large-v3-turbo", state.sttModel)
        assertEquals("anthropic/claude-3-haiku", state.llmModel)
    }

    @Test
    fun `activeProviders excludes providers without active token`() = runBlocking {
        // Setup: Groq has token, OpenRouter has old flat key, ZAI has nothing
        coEvery { appSettingsStore.getActiveAuthToken("groq") } returns "token_groq"
        coEvery { appSettingsStore.getActiveAuthToken("openrouter") } returns "token_or"
        coEvery { appSettingsStore.getActiveAuthToken("zai") } returns null
        
        // Need to recreate ViewModel to trigger init { loadSettings() } with new mocks
        viewModel = SettingsViewModel(deleteUseCase, repository, appSettingsStore, validateApiKeysUseCase, providerModelDao, context)
        testDispatcher.scheduler.runCurrent()
        
        val state = viewModel.uiState.value
        
        assertEquals(true, state.providerAuthStatus["groq"])
        assertEquals(true, state.providerAuthStatus["openrouter"])
        assertEquals(false, state.providerAuthStatus["zai"])
        
        assertTrue(state.activeProviders.contains("groq"))
        assertTrue(state.activeProviders.contains("openrouter"))
        assertFalse(state.activeProviders.contains("zai"))
        
        assertFalse(state.availableProviders.contains("groq"))
        assertTrue(state.availableProviders.contains("zai"))
    }
    
    @Test
    fun `changing provider updates available models from dao`() = runBlocking {
        val orModels = listOf(ModelEntity(externalId = "model1", providerId = "openrouter", modelName = "Model 1"))
        coEvery { providerModelDao.getModelsForProvider("openrouter") } returns orModels
        
        testDispatcher.scheduler.runCurrent()
        
        viewModel.onLlmProviderChanged("openrouter")
        testDispatcher.scheduler.runCurrent()
        
        val state = viewModel.uiState.value
        assertEquals("openrouter", state.llmProvider)
        assertEquals(orModels, state.llmAvailableModels)
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

    @Test
    fun `saveSettings retries queued transcriptions with new model`() = runBlocking {
        repository.insert(
            TranscriptionEntity(
                id = 10,
                originalText = "",
                processedText = null,
                audioFilePath = "/a.m4a",
                sttModel = "old",
                llmModel = "llm",
                createdAt = "2026-01-01T00:00:00",
                postProcessingType = "RAW",
                status = TranscriptionStatus.STT_ERROR_PERMANENT.name,
                errorMessage = "Model invalid"
            )
        )
        coEvery { appSettingsStore.getActiveAuthToken("groq") } returns "groq-key"
        coEvery { appSettingsStore.getActiveAuthToken("openrouter") } returns "or-key"

        viewModel.onSttModelChanged("whisper-large-v3-turbo")
        viewModel.onLlmModelChanged("inception/mercury")
        viewModel.saveSettings()
        testDispatcher.scheduler.runCurrent()

        assertEquals("whisper-large-v3-turbo", repository.getById(1)?.sttModel)
    }
}
