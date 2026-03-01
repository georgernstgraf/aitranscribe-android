package com.georgernstgraf.aitranscribe.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from GROQ transcription API.
 */
data class GroqTranscriptionResponse(
    @SerializedName("text")
    val text: String
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