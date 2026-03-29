package com.georgernstgraf.aitranscribe.ui.viewmodel

import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError
import com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyValidationResult
import com.georgernstgraf.aitranscribe.domain.usecase.ValidateApiKeysUseCase
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private lateinit var securePreferences: SecurePreferences
    private lateinit var validateApiKeysUseCase: ValidateApiKeysUseCase
    private lateinit var viewModel: SetupViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        securePreferences = mockk(relaxed = true)
        validateApiKeysUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun createViewModel(): SetupViewModel {
        coEvery { securePreferences.getGroqApiKey() } returns null
        coEvery { securePreferences.getOpenRouterApiKey() } returns null
        val vm = SetupViewModel(securePreferences, validateApiKeysUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        return vm
    }

    @Test
    fun `initial state has default values`() = runBlocking {
        coEvery { securePreferences.getGroqApiKey() } returns null
        coEvery { securePreferences.getOpenRouterApiKey() } returns null

        viewModel = SetupViewModel(securePreferences, validateApiKeysUseCase)

        val state = viewModel.uiState.value
        assertNull(state.groqApiKey)
        assertNull(state.openRouterApiKey)
        assertFalse(state.isSetupComplete)
        assertFalse(state.isValidating)
    }

    @Test
    fun `loadExistingKeys populates keys from secure preferences`() = runBlocking {
        coEvery { securePreferences.getGroqApiKey() } returns "gsk_test1234567890123456"
        coEvery { securePreferences.getOpenRouterApiKey() } returns "sk-or-test1234567890123"
        coEvery {
            validateApiKeysUseCase("gsk_test1234567890123456", "sk-or-test1234567890123")
        } returns ApiKeyValidationResult(
            isGroqKeyValid = true,
            isOpenRouterKeyValid = true,
            groqKeyError = null,
            openRouterKeyError = null,
            isValid = true
        )

        viewModel = SetupViewModel(securePreferences, validateApiKeysUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("gsk_test1234567890123456", state.groqApiKey)
        assertEquals("sk-or-test1234567890123", state.openRouterApiKey)
        assertTrue(state.isSetupComplete)
    }

    @Test
    fun `onGroqApiKeyChanged updates groq key and clears validation`() = runBlocking {
        viewModel = createViewModel()

        viewModel.onGroqApiKeyChanged("new-key")

        val state = viewModel.uiState.value
        assertEquals("new-key", state.groqApiKey)
        assertNull(state.groqKeyError)
        assertNull(state.isGroqKeyValid)
    }

    @Test
    fun `onOpenRouterApiKeyChanged updates openrouter key and clears validation`() = runBlocking {
        viewModel = createViewModel()

        viewModel.onOpenRouterApiKeyChanged("new-or-key")

        val state = viewModel.uiState.value
        assertEquals("new-or-key", state.openRouterApiKey)
        assertNull(state.openRouterKeyError)
        assertNull(state.isOpenRouterKeyValid)
    }

    @Test
    fun `validateAndSave sets isSetupComplete on success`() = runBlocking {
        coEvery { securePreferences.setGroqApiKey(any()) } returns Unit
        coEvery { securePreferences.setOpenRouterApiKey(any()) } returns Unit
        coEvery {
            validateApiKeysUseCase("gsk_test1234567890123456", "sk-or-test1234567890123")
        } returns ApiKeyValidationResult(
            isGroqKeyValid = true,
            isOpenRouterKeyValid = true,
            groqKeyError = null,
            openRouterKeyError = null,
            isValid = true
        )

        viewModel = createViewModel()

        viewModel.onGroqApiKeyChanged("gsk_test1234567890123456")
        viewModel.onOpenRouterApiKeyChanged("sk-or-test1234567890123")
        viewModel.validateAndSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSetupComplete)
        assertFalse(state.isValidating)
    }

    @Test
    fun `validateAndSave shows error on validation failure`() = runBlocking {
        coEvery {
            validateApiKeysUseCase(any(), any())
        } returns ApiKeyValidationResult(
            isGroqKeyValid = false,
            isOpenRouterKeyValid = false,
            groqKeyError = ApiKeyError.MISSING,
            openRouterKeyError = ApiKeyError.MISSING,
            isValid = false
        )

        viewModel = createViewModel()

        viewModel.validateAndSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSetupComplete)
        assertFalse(state.isValidating)
        assertNotNull(state.errorMessage)
        assertEquals(ApiKeyError.MISSING, state.groqKeyError)
    }
}
