package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterMessage
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterRequest
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PostProcessTextUseCase @Inject constructor(
    private val openRouterApiService: OpenRouterApiService,
    private val repository: TranscriptionRepository
) {

    suspend operator fun invoke(
        transcriptionId: Long,
        postProcessingType: PostProcessingType,
        llmModel: String,
        apiKey: String
    ) = withContext(Dispatchers.IO) {
        if (postProcessingType == PostProcessingType.RAW) return@withContext

        if (apiKey.isBlank()) {
            throw PostProcessingException("API key cannot be empty")
        }

        val transcription = repository.getById(transcriptionId)
            ?: throw PostProcessingException("Transcription not found: $transcriptionId")

        try {
            val systemPrompt = buildSystemPrompt(postProcessingType)

            val request = OpenRouterRequest(
                model = llmModel,
                messages = listOf(
                    OpenRouterMessage(
                        role = "system",
                        content = systemPrompt
                    ),
                    OpenRouterMessage(
                        role = "user",
                        content = "Here is the transcription:\n\n${transcription.originalText}"
                    )
                )
            )

            val response = openRouterApiService.processText(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (!response.isSuccessful || response.body() == null) {
                throw PostProcessingException(
                    message = "Post-processing failed: ${response.message()}",
                    errorCode = response.code()
                )
            }

            val processedText = response.body()!!.getContent()

            repository.update(
                transcription.copy(
                    processedText = processedText,
                    postProcessingType = postProcessingType.name,
                    status = TranscriptionStatus.COMPLETED.name
                )
            )
        } catch (e: PostProcessingException) {
            repository.recordError(transcriptionId, e.message ?: "Unknown error")
            throw e
        } catch (e: Exception) {
            repository.recordError(transcriptionId, "Post-processing failed: ${e.message}")
            throw PostProcessingException("Failed to post-process text: ${e.message}", e)
        }
    }

    suspend fun generateSummary(
        transcriptionId: Long,
        llmModel: String,
        apiKey: String
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext

        val transcription = repository.getById(transcriptionId) ?: return@withContext
        val text = transcription.processedText ?: transcription.originalText
        if (text.isBlank()) return@withContext

        try {
            val request = OpenRouterRequest(
                model = llmModel,
                messages = listOf(
                    OpenRouterMessage(
                        role = "system",
                        content = "Create a concise summary of the transcription in 70 to 80 characters. Output only the summary text with no quotes, labels, or extra commentary."
                    ),
                    OpenRouterMessage(
                        role = "user",
                        content = text
                    )
                )
            )

            val response = openRouterApiService.processText(
                authorization = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                val summary = response.body()!!.getContent().trim()
                if (summary.isNotBlank()) {
                    repository.updateSummary(transcriptionId, summary)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun buildSystemPrompt(type: PostProcessingType): String {
        return when (type) {
            PostProcessingType.RAW -> ""
            PostProcessingType.CLEANUP -> """
                You are a helpful assistant analyzing an audio transcription.
                IMPORTANT: Output ONLY the requested processed text.
                Do not include any introductory remarks, explanations,
                or concluding comments (like 'Here is the processed text').
                
                Please correct grammatical errors, remove filler words,
                and structure the following text clearly.
            """.trimIndent()

            PostProcessingType.ENGLISH -> """
                You are a helpful assistant analyzing an audio transcription.
                IMPORTANT: Output ONLY the requested processed text.
                Do not include any introductory remarks, explanations,
                or concluding comments (like 'Here is the translation').
                
                Please translate the following text to English,
                correct grammatical errors, remove filler words,
                and structure it clearly.
            """.trimIndent()
        }
    }

    class PostProcessingException(
        message: String,
        cause: Throwable? = null,
        val errorCode: Int? = null
    ) : Exception(message, cause)
}
