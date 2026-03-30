package com.georgernstgraf.aitranscribe.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConfigTest {

    @Test
    fun `stt models contains whisper-large-v3-turbo and whisper-large-v3`() {
        assertTrue(ProviderConfig.sttModels.contains("whisper-large-v3-turbo"))
        assertTrue(ProviderConfig.sttModels.contains("whisper-large-v3"))
        assertEquals(2, ProviderConfig.sttModels.size)
    }

    @Test
    fun `llm providers contain openrouter and zai`() {
        assertEquals(2, ProviderConfig.llmProviders.size)
        assertEquals("openrouter", ProviderConfig.llmProviders[0].id)
        assertEquals("zai", ProviderConfig.llmProviders[1].id)
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
        assertEquals(listOf("openrouter", "zai"), ids)
    }
}
