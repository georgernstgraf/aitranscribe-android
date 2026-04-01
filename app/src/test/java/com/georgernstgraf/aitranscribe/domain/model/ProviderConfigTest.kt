package com.georgernstgraf.aitranscribe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProviderConfigTest {

    @Test
    fun `stt models contains expected models`() {
        val models = ProviderConfig.sttModels
        assertTrue(models.contains("whisper-large-v3-turbo"))
        assertTrue(models.contains("whisper-large-v3"))
        assertTrue(models.contains("openai/whisper-large-v3"))
        assertTrue(models.contains("google/gemini-2.0-flash-001"))
        assertTrue(models.contains("glm-asr-2512"))
    }

    @Test
    fun `llm providers contain expected providers`() {
        val providers = ProviderConfig.llmProviders
        assertEquals(3, providers.size)
        assertEquals("openrouter", providers[0].id)
        assertEquals("zai", providers[1].id)
        assertEquals("groq", providers[2].id)
    }

    @Test
    fun `getLlmModelsForProvider returns correct models`() {
        val openRouterModels = ProviderConfig.getLlmModelsForProvider("openrouter")
        assertTrue(openRouterModels.contains("inception/mercury"))
        assertTrue(openRouterModels.contains("google/gemini-2.5-flash-lite"))
        assertTrue(openRouterModels.contains("anthropic/claude-3-haiku"))

        val zaiModels = ProviderConfig.getLlmModelsForProvider("zai")
        assertTrue(zaiModels.contains("glm-4.7-flash"))
        assertTrue(zaiModels.contains("glm-4.5-flash"))
        assertTrue(zaiModels.contains("glm-4.7"))
    }

    @Test
    fun `getLlmModelsForProvider returns empty for unknown provider`() {
        assertTrue(ProviderConfig.getLlmModelsForProvider("unknown").isEmpty())
    }

    @Test
    fun `getLlmProviderForModel returns correct provider`() {
        assertEquals("openrouter", ProviderConfig.getLlmProviderForModel("inception/mercury"))
        assertEquals("openrouter", ProviderConfig.getLlmProviderForModel("anthropic/claude-3-haiku"))
        assertEquals("zai", ProviderConfig.getLlmProviderForModel("glm-4.7-flash"))
        assertNull(ProviderConfig.getLlmProviderForModel("nonexistent-model"))
    }

    @Test
    fun `getLlmProviderDisplayName returns correct names`() {
        assertEquals("OpenRouter", ProviderConfig.getLlmProviderDisplayName("openrouter"))
        assertEquals("ZAI", ProviderConfig.getLlmProviderDisplayName("zai"))
        assertEquals("unknown", ProviderConfig.getLlmProviderDisplayName("unknown"))
    }

    @Test
    fun `getDefaultLlmModel returns first model for provider`() {
        assertEquals("inception/mercury", ProviderConfig.getDefaultLlmModel("openrouter"))
        assertEquals("glm-4.7-flash", ProviderConfig.getDefaultLlmModel("zai"))
    }

    @Test
    fun `isValidLlmModel works correctly`() {
        assertTrue(ProviderConfig.isValidLlmModel("openrouter", "inception/mercury"))
        assertFalse(ProviderConfig.isValidLlmModel("openrouter", "glm-4.7-flash"))
        assertTrue(ProviderConfig.isValidLlmModel("zai", "glm-4.7-flash"))
        assertFalse(ProviderConfig.isValidLlmModel("zai", "inception/mercury"))
    }

    @Test
    fun `isValidSttModel works correctly`() {
        assertTrue(ProviderConfig.isValidSttModel("groq", "whisper-large-v3-turbo"))
        assertTrue(ProviderConfig.isValidSttModel("groq", "whisper-large-v3"))
        assertFalse(ProviderConfig.isValidSttModel("groq", "glm-asr-2512"))
    }

    @Test
    fun `openrouter models include gemini 2_5 flash lite`() {
        val models = ProviderConfig.getLlmModelsForProvider("openrouter")
        assertTrue(models.contains("google/gemini-2.5-flash-lite"))
    }

    @Test
    fun `zai models are all fast or free tier`() {
        val models = ProviderConfig.getLlmModelsForProvider("zai")
        assertTrue(models.contains("glm-4.7-flash"))
        assertTrue(models.contains("glm-4.5-flash"))
        assertTrue(models.contains("glm-4-32b-0414-128k"))
        assertTrue(models.contains("glm-4.7-flashx"))
        assertTrue(models.contains("glm-4.5-air"))
        assertTrue(models.contains("glm-4.7"))
        assertEquals(6, models.size)
    }

    @Test
    fun `llmProviderIds returns list of provider ids`() {
        val ids = ProviderConfig.llmProviderIds
        assertEquals(3, ids.size)
        assertTrue(ids.contains("openrouter"))
        assertTrue(ids.contains("zai"))
        assertTrue(ids.contains("groq"))
    }
}
