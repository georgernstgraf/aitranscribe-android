package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.testing.TestEnv
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateApiKeysIntegrationTest {

    private lateinit var useCase: ValidateApiKeysUseCase

    @Before
    fun setup() {
        TestEnv.requireApiIntegration()
        useCase = ValidateApiKeysUseCase(OkHttpClient())
    }

    @Test
    fun `real groq key validates against live API`() = runBlocking {
        val groqKey = TestEnv.getGroqApiKey()
        val openRouterKey = TestEnv.getOpenRouterApiKey()

        val result = useCase(groqKey, openRouterKey)

        assertTrue("API key validation failed. Check .env values.", result.isValid)
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

        assertTrue("Model validation failed. Check GROQ_STT_MODEL and OPENROUTER_LLM_MODEL in .env.", result.isValid)
        assertNull(result.sttModelError)
        assertNull(result.llmModelError)
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
