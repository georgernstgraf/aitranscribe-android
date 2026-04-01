package com.georgernstgraf.aitranscribe.domain.usecase

import android.util.Log
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiCodingApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterMessage
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterRequest
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterResponse
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.TranslationTarget
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.georgernstgraf.aitranscribe.domain.util.PromptManager
import retrofit2.Response
import javax.inject.Inject

class PostProcessTextUseCase @Inject constructor(
    private val openRouterApiService: OpenRouterApiService,
    private val zaiApiService: ZaiApiService,
    private val zaiCodingApiService: ZaiCodingApiService,
    private val groqApiService: GroqApiService,
    private val repository: TranscriptionRepository,
    private val promptManager: PromptManager
) {

    suspend operator fun invoke(
        transcriptionId: Long,
        postProcessingType: PostProcessingType,
        llmModel: String,
        apiKey: String,
        llmProvider: String = "openrouter",
        debugContext: String = "worker:$postProcessingType"
    ) = withContext(Dispatchers.IO) {
        when (postProcessingType) {
            PostProcessingType.RAW -> return@withContext
            PostProcessingType.CLEANUP -> runPostProcessing(
                transcriptionId = transcriptionId,
                llmModel = llmModel,
                apiKey = apiKey,
                llmProvider = llmProvider,
                prompt = buildCleanupPrompt("prompt.cleanup.null"),
                debugContext = debugContext
            )
            PostProcessingType.TRANSLATE_TO_EN -> runPostProcessing(
                transcriptionId = transcriptionId,
                llmModel = llmModel,
                apiKey = apiKey,
                llmProvider = llmProvider,
                prompt = buildTranslationPrompt(TranslationTarget.EN),
                debugContext = debugContext
            )
            PostProcessingType.TRANSLATE_TO_DE -> runPostProcessing(
                transcriptionId = transcriptionId,
                llmModel = llmModel,
                apiKey = apiKey,
                llmProvider = llmProvider,
                prompt = buildTranslationPrompt(TranslationTarget.DE),
                debugContext = debugContext
            )
        }
    }

    suspend operator fun invoke(
        transcriptionId: Long,
        isCleanupEnabled: Boolean,
        translationTarget: TranslationTarget,
        llmModel: String,
        apiKey: String,
        llmProvider: String = "openrouter"
    ) = withContext(Dispatchers.IO) {
        val transcription = repository.getById(transcriptionId)
            ?: throw PostProcessingException("Transcription not found: $transcriptionId")
        val currentLanguage = transcription.language?.lowercase()

        val (prompt, resultingLanguage) = resolveDetailPrompt(
            language = currentLanguage,
            button = translationTarget,
            cleanup = isCleanupEnabled
        )
        if (prompt == null) return@withContext

        runPostProcessing(
            transcriptionId = transcriptionId,
            llmModel = llmModel,
            apiKey = apiKey,
            llmProvider = llmProvider,
            prompt = prompt,
            resultingLanguage = resultingLanguage,
            debugContext = "detail:${translationTarget.name.lowercase()} cleanup=$isCleanupEnabled language=${currentLanguage ?: "unknown"}"
        )

        generateSummary(transcriptionId, llmModel, apiKey, llmProvider, "detail:summary")
    }

    suspend fun generateSummary(
        transcriptionId: Long,
        llmModel: String,
        apiKey: String,
        llmProvider: String = "openrouter",
        debugContext: String = "summary"
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext

        val transcription = repository.getById(transcriptionId) ?: return@withContext
        val text = transcription.text.orEmpty()
        val language = transcription.language
        if (text.isBlank()) return@withContext

        try {
            val systemContent = buildSummaryPrompt(language)
            val userContent = buildSummaryUserContent(text)
            recordPromptPreview(
                context = debugContext,
                systemPrompt = systemContent,
                userPrompt = buildSummaryUserContent(PROMPT_TEXT_PLACEHOLDER)
            )
            val request = OpenRouterRequest(
                model = llmModel,
                messages = listOf(
                    OpenRouterMessage(
                        role = "system",
                        content = systemContent
                    ),
                    OpenRouterMessage(
                        role = "user",
                        content = userContent
                    )
                )
            )

            val response = callLlmApi(llmProvider, apiKey, request)
            if (response.isSuccessful && response.body() != null) {
                val summary = response.body()!!.getContent().trim()
                if (summary.isNotBlank()) {
                    repository.updateSummary(transcriptionId, summary)
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun runPostProcessing(
        transcriptionId: Long,
        llmModel: String,
        apiKey: String,
        llmProvider: String,
        prompt: String,
        resultingLanguage: String? = null,
        debugContext: String = "postprocess"
    ) {
        if (apiKey.isBlank()) {
            throw PostProcessingException("API key cannot be empty")
        }

        val transcription = repository.getById(transcriptionId)
            ?: throw PostProcessingException("Transcription not found: $transcriptionId")
        val sourceText = transcription.text.orEmpty()
        if (sourceText.isBlank()) {
            throw PostProcessingException("Cannot post-process empty transcription")
        }

        try {
            val systemContent = buildSystemPrompt(prompt)
            val userContent = buildTranscriptionUserContent(sourceText)
            recordPromptPreview(
                context = debugContext,
                systemPrompt = systemContent,
                userPrompt = buildTranscriptionUserContent(PROMPT_TEXT_PLACEHOLDER)
            )
            val request = OpenRouterRequest(
                model = llmModel,
                messages = listOf(
                    OpenRouterMessage(role = "system", content = systemContent),
                    OpenRouterMessage(
                        role = "user",
                        content = userContent
                    )
                )
            )

            val response = callLlmApi(llmProvider, apiKey, request)
            if (!response.isSuccessful || response.body() == null) {
                val errorBody = response.errorBody()?.string()?.take(200) ?: "no body"
                throw PostProcessingException(
                    message = "Post-processing failed: HTTP ${response.code()} - $errorBody",
                    errorCode = response.code()
                )
            }

            val processedText = response.body()!!.getContent().trim()
            val updatedLanguage = resultingLanguage ?: transcription.language
            repository.update(
                transcription.copy(
                    text = processedText,
                    status = TranscriptionStatus.COMPLETED.name,
                    errorMessage = null,
                    language = updatedLanguage
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

    private suspend fun callLlmApi(
        provider: String,
        apiKey: String,
        request: OpenRouterRequest
    ): Response<OpenRouterResponse> {
        val authorization = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        return when (provider) {
            "groq" -> groqApiService.processText(authorization, request)
            "zai" -> {
                val primary = zaiApiService.processText(authorization, request)
                if (shouldRetryZaiWithCodingEndpoint(primary)) {
                    zaiCodingApiService.processText(authorization, request)
                } else {
                    primary
                }
            }
            else -> openRouterApiService.processText(authorization, request)
        }
    }

    private fun shouldRetryZaiWithCodingEndpoint(response: Response<OpenRouterResponse>): Boolean {
        if (response.code() != 429) return false
        val errorText = runCatching { response.errorBody()?.string() }
            .getOrNull()
            ?.lowercase()
            ?: return true
        return errorText.contains("insufficient balance") ||
            errorText.contains("no resource package") ||
            errorText.contains("resource package")
    }

    private fun buildCleanupPrompt(languageDirectiveKey: String): String {
        return "${promptManager.get("prompt.cleanup")}\n${promptManager.get(languageDirectiveKey)}"
    }

    private fun buildTranslationPrompt(target: TranslationTarget): String {
        return when (target) {
            TranslationTarget.EN -> promptManager.get("prompt.translate.en")
            TranslationTarget.DE -> promptManager.get("prompt.translate.de")
            TranslationTarget.NONE -> ""
        }
    }

    private fun resolveDetailPrompt(
        language: String?,
        button: TranslationTarget,
        cleanup: Boolean
    ): Pair<String?, String?> {
        val targetLanguage = when (button) {
            TranslationTarget.EN -> "en"
            TranslationTarget.DE -> "de"
            TranslationTarget.NONE -> language
        }
        if (button == TranslationTarget.NONE) {
            if (!cleanup) return null to language
            val directiveKey = when (language) {
                "en" -> "prompt.cleanup.en"
                "de" -> "prompt.cleanup.de"
                else -> "prompt.cleanup.null"
            }
            return buildCleanupPrompt(directiveKey) to language
        }

        if (!cleanup) {
            return if (language == targetLanguage) {
                null to language
            } else {
                buildTranslationPrompt(button) to targetLanguage
            }
        }

        return if (language == targetLanguage && language != null) {
            buildCleanupPrompt("prompt.cleanup.$language") to language
        } else {
            val directiveKey = when (targetLanguage) {
                "en" -> "prompt.cleanup.en"
                "de" -> "prompt.cleanup.de"
                else -> "prompt.cleanup.null"
            }
            buildCleanupPrompt(directiveKey) to targetLanguage
        }
    }

    private fun buildSummaryUserContent(text: String): String {
        return buildTranscriptionUserContent(text)
    }

    private fun buildSystemPrompt(userRequest: String): String {
        val base = promptManager.get("prompt.system.base")
        val request = promptManager.get("prompt.system.request").replace("{{request}}", userRequest)
        return "$base\n\n$request"
    }

    private fun buildTranscriptionUserContent(text: String): String {
        return promptManager.get("prompt.user.transcription").replace("{{text}}", text)
    }

    private fun buildSummaryPrompt(language: String?): String {
        val basePrompt = promptManager.get("prompt.summary")
        return if (language != null) {
            val languageName = getLanguageDisplayName(language)
            basePrompt.replace("{{language}}", languageName)
        } else {
            basePrompt.replace("{{language}}", "the original language of the text")
        }
    }

    private fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "de", "german" -> "German"
            "en", "english" -> "English"
            "fr", "french" -> "French"
            "es", "spanish" -> "Spanish"
            "it", "italian" -> "Italian"
            "pt", "portuguese" -> "Portuguese"
            "nl", "dutch" -> "Dutch"
            "pl", "polish" -> "Polish"
            "ru", "russian" -> "Russian"
            "ja", "japanese" -> "Japanese"
            "zh", "chinese" -> "Chinese"
            "ko", "korean" -> "Korean"
            "ar", "arabic" -> "Arabic"
            "hi", "hindi" -> "Hindi"
            "tr", "turkish" -> "Turkish"
            "sv", "swedish" -> "Swedish"
            "da", "danish" -> "Danish"
            "no", "norwegian" -> "Norwegian"
            "fi", "finnish" -> "Finnish"
            "cs", "czech" -> "Czech"
            "hu", "hungarian" -> "Hungarian"
            "ro", "romanian" -> "Romanian"
            "el", "greek" -> "Greek"
            "he", "hebrew" -> "Hebrew"
            "th", "thai" -> "Thai"
            "vi", "vietnamese" -> "Vietnamese"
            "id", "indonesian" -> "Indonesian"
            "ms", "malay" -> "Malay"
            "uk", "ukrainian" -> "Ukrainian"
            "bg", "bulgarian" -> "Bulgarian"
            "hr", "croatian" -> "Croatian"
            "sr", "serbian" -> "Serbian"
            "sk", "slovak" -> "Slovak"
            "sl", "slovenian" -> "Slovenian"
            "lt", "lithuanian" -> "Lithuanian"
            "lv", "latvian" -> "Latvian"
            "et", "estonian" -> "Estonian"
            else -> languageCode.uppercase()
        }
    }

    private suspend fun recordPromptPreview(context: String, systemPrompt: String, userPrompt: String) {
        val preview = """
[Prompt Preview]
context=$context
system:
$systemPrompt

user:
$userPrompt
""".trimIndent()
        Log.i(PROMPT_DEBUG_TAG, preview)
    }

    companion object {
        private const val PROMPT_DEBUG_TAG = "PromptDebug"
        private const val PROMPT_TEXT_PLACEHOLDER = "{{TEXT}}"
    }

    class PostProcessingException(
        message: String,
        cause: Throwable? = null,
        val errorCode: Int? = null
    ) : Exception(message, cause)
}
