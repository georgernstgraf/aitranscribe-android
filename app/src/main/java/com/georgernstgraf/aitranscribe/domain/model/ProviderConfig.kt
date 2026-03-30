package com.georgernstgraf.aitranscribe.domain.model

object ProviderConfig {

    data class Provider(
        val id: String,
        val displayName: String,
        val models: List<String>
    )

    val sttProviders = listOf(
        Provider(
            id = "groq",
            displayName = "Groq",
            models = listOf(
                "whisper-large-v3-turbo",
                "whisper-large-v3"
            )
        )
    )

    val sttModels = sttProviders.flatMap { it.models }.distinct()

    val llmProviders = listOf(
        Provider(
            id = "openrouter",
            displayName = "OpenRouter",
            models = listOf(
                "inception/mercury",
                "google/gemini-2.5-flash-lite",
                "google/gemini-2.0-flash-001",
                "anthropic/claude-3-haiku",
                "mistralai/mistral-small-3.1-24b-instruct",
                "google/gemma-3-12b-it",
                "meta-llama/llama-3.3-70b-instruct",
                "meta-llama/llama-4-scout"
            )
        ),
        Provider(
            id = "zai",
            displayName = "ZAI",
            models = listOf(
                "glm-4.7-flash",
                "glm-4.5-flash",
                "glm-4-32b-0414-128k",
                "glm-4.7-flashx",
                "glm-4.5-air",
                "glm-4.7"
            )
        )
    )

    val llmProviderIds = llmProviders.map { it.id }

    fun getLlmProviderDisplayName(providerId: String): String {
        return llmProviders.find { it.id == providerId }?.displayName ?: providerId
    }

    fun getLlmModelsForProvider(providerId: String): List<String> {
        return llmProviders.find { it.id == providerId }?.models ?: emptyList()
    }

    fun getLlmProviderForModel(model: String): String? {
        return llmProviders.find { model in it.models }?.id
    }

    fun getDefaultSttModel(providerId: String): String {
        return getSttModelsForProvider(providerId).firstOrNull() ?: "whisper-large-v3-turbo"
    }

    fun getDefaultLlmModel(providerId: String): String {
        return getLlmModelsForProvider(providerId).firstOrNull() ?: "anthropic/claude-3-haiku"
    }

    fun isValidLlmModel(providerId: String, model: String): Boolean {
        return model in getLlmModelsForProvider(providerId)
    }

    fun getSttModelsForProvider(providerId: String): List<String> {
        return sttProviders.find { it.id == providerId }?.models ?: emptyList()
    }

    fun getSttProviderDisplayName(providerId: String): String {
        return sttProviders.find { it.id == providerId }?.displayName ?: providerId
    }

    fun isValidSttModel(providerId: String, model: String): Boolean {
        return model in getSttModelsForProvider(providerId)
    }
}
