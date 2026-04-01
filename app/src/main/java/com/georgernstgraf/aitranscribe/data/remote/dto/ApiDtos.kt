package com.georgernstgraf.aitranscribe.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from GROQ transcription API.
 */
data class GroqTranscriptionResponse(
    @SerializedName("text")
    val text: String,
    @SerializedName("language")
    val language: String? = null
)

/**
 * Response from OpenRouter API.
 */
data class OpenRouterResponse(
    @SerializedName("choices")
    val choices: List<OpenRouterChoice>
) {
    fun getContent(): String = choices.firstOrNull()?.message?.content ?: ""
}

data class OpenRouterChoice(
    @SerializedName("message")
    val message: OpenRouterMessage
)

data class OpenRouterMessage(
    @SerializedName("role")
    val role: String,
    @SerializedName("content")
    val content: String
)

/**
 * Request for OpenRouter API.
 */
data class OpenRouterRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("messages")
    val messages: List<OpenRouterMessage>
)

/**
 * Standard OpenAI-compatible /v1/models response.
 */
data class ModelsResponse(
    @SerializedName("data")
    val data: List<ModelDto>
)

data class ModelDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String? = null, // OpenRouter uses name sometimes
    @SerializedName("architecture")
    val architecture: ModelArchitectureDto? = null
)

data class ModelArchitectureDto(
    @SerializedName("modality")
    val modality: String? = null,
    @SerializedName("instruct_type")
    val instructType: String? = null
)