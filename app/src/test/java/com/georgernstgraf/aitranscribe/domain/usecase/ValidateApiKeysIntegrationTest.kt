package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.testing.TestEnv
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidateApiKeysIntegrationTest {

    private lateinit var useCase: ValidateApiKeysUseCase

    @BeforeEach
    fun setup() {
        TestEnv.requireApiIntegration()
        useCase = ValidateApiKeysUseCase(OkHttpClient())
    }

    @Test
    fun `real groq key validates against live API`() = runBlocking {
        val groqKey = TestEnv.getGroqApiKey()
        val openRouterKey = TestEnv.getOpenRouterApiKey()

        val result = useCase(groqKey, openRouterKey)

        assertTrue(result.isValid, "API key validation failed. Check .env values.")
        assertTrue(result.isGroqKeyValid)
        assertTrue(result.isOpenRouterKeyValid)
        assertNull(result.groqKeyError)
        assertNull(result.openRouterKeyError)
    }

    @Test
    fun `invalid groq key fails live API validation`() = runBlocking {
        val openRouterKey = TestEnv.getOpenRouterApiKey()

        val result = useCase("gsk_invalid_key_for_testing_12345", openRouterKey)

        assertFalse(result.isValid)
        assertFalse(result.isGroqKeyValid)
        assertNotNull(result.groqKeyError)
    }

    @Test
    fun `invalid openrouter key fails live API validation`() = runBlocking {
        val groqKey = TestEnv.getGroqApiKey()

        val result = useCase(groqKey, "sk-or-invalid_key_for_testing_12345")

        assertFalse(result.isValid)
        assertFalse(result.isOpenRouterKeyValid)
        assertNotNull(result.openRouterKeyError)
    }

    @Test
    fun `real models validate against live API`() = runBlocking {
        val groqKey = TestEnv.getGroqApiKey()
        val openRouterKey = TestEnv.getOpenRouterApiKey()
        val sttModel = TestEnv.getSttModel()
        val llmModel = TestEnv.getLlmModel()

        val result = useCase.validateModels(groqKey, openRouterKey, sttModel, llmModel)

        assertEquals(null, result.sttModelError)
        assertEquals(null, result.llmModelError)
        assertTrue(result.isValid, "Model validation failed. Check GROQ_STT_MODEL and OPENROUTER_LLM_MODEL in .env.")
    }

    @Test
    fun `unknown STT model fails validation`() = runBlocking {
        val groqKey = TestEnv.getGroqApiKey()
        val openRouterKey = TestEnv.getOpenRouterApiKey()
        val llmModel = TestEnv.getLlmModel()

        val result = useCase.validateModels(groqKey, openRouterKey, "nonexistent-model-v99", llmModel)

        assertFalse(result.isValid)
        assertNotNull(result.sttModelError)
    }

    @Test
    fun `unknown LLM model fails validation`() = runBlocking {
        val groqKey = TestEnv.getGroqApiKey()
        val openRouterKey = TestEnv.getOpenRouterApiKey()
        val sttModel = TestEnv.getSttModel()

        val result = useCase.validateModels(groqKey, openRouterKey, sttModel, "nonexistent/llm-model-v99")

        assertFalse(result.isValid)
        assertNotNull(result.llmModelError)
    }
}
