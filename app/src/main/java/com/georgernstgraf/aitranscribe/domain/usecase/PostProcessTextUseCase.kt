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
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepository
import com.georgernstgraf.aitranscribe.domain.util.PromptManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject

class PostProcessTextUseCase @Inject constructor(
    private val openRouterApiService: OpenRouterApiService,
    private val zaiApiService: ZaiApiService,
    private val zaiCodingApiService: ZaiCodingApiService,
    private val groqApiService: GroqApiService,
    private val repository: TranscriptionRepository,
    private val promptManager: PromptManager,
    private val languageRepository: LanguageRepository
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
            PostProcessingType.CLEANUP -> {
                val entity = repository.getById(transcriptionId) ?: return@withContext
                val languageId = entity.languageId
                val languageName = languageId?.let { id -> languageRepository.getLanguageName(id) }
                    ?: "the same language as the input"
                runPostProcessing(
                    transcriptionId = transcriptionId,
                    llmModel = llmModel,
                    apiKey = apiKey,
                    llmProvider = llmProvider,
                    prompt = buildCleanupPrompt(languageName),
                    debugContext = debugContext
                )
            }
            else -> return@withContext
        }
    }

    suspend operator fun invoke(
        transcriptionId: Long,
        isCleanupEnabled: Boolean,
        targetLanguage: String?,  // null = cleanup only, "en"/"de" = translate
        llmModel: String,
        apiKey: String,
        llmProvider: String = "openrouter"
    ) = withContext(Dispatchers.IO) {
        val entity = repository.getById(transcriptionId)
            ?: throw PostProcessingException("Transcription not found: $transcriptionId")
        val currentLanguage = entity.languageId?.lowercase()

        val (prompt, resultingLanguage) = resolveDetailPrompt(
            sourceLanguage = currentLanguage,
            targetLanguage = targetLanguage,
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
            debugContext = "detail:target=${targetLanguage ?: "cleanup"} cleanup=$isCleanupEnabled language=${currentLanguage ?: "unknown"}"
        )

        generateSummary(transcriptionId, llmModel, apiKey, llmProvider, "detail:summary")
    }

    suspend fun cleanupTranscription(
        transcriptionId: Long,
        llmModel: String,
        apiKey: String,
        llmProvider: String = "openrouter"
    ) = withContext(Dispatchers.IO) {
        val entity = repository.getById(transcriptionId)
            ?: throw PostProcessingException("Transcription not found: $transcriptionId")

        val languageId = entity.languageId
        val languageName = languageId?.let { languageRepository.getLanguageName(it) }
            ?: "the same language as the input"

        val prompt = buildCleanupPrompt(languageName)

        runPostProcessing(
            transcriptionId = transcriptionId,
            llmModel = llmModel,
            apiKey = apiKey,
            llmProvider = llmProvider,
            prompt = prompt,
            resultingLanguage = languageId,
            debugContext = "detail:cleanup language=${languageId ?: "unknown"}"
        )
    }

    suspend fun generateSummary(
        transcriptionId: Long,
        llmModel: String,
        apiKey: String,
        llmProvider: String = "openrouter",
        debugContext: String = "summary"
    ) = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext

        val entity = repository.getById(transcriptionId) ?: return@withContext
        val text = entity.sttText.orEmpty()
        val language = entity.languageId
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
                    OpenRouterMessage(role = "system", content = systemContent),
                    OpenRouterMessage(role = "user", content = userContent)
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

        val entity = repository.getById(transcriptionId)
            ?: throw PostProcessingException("Transcription not found: $transcriptionId")
        val sourceText = entity.sttText.orEmpty()
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
                    OpenRouterMessage(role = "user", content = userContent)
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
            repository.updateCleanedText(transcriptionId, processedText)
            if (resultingLanguage != null) {
                repository.updateLanguage(transcriptionId, resultingLanguage)
            }
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

    private suspend fun buildCleanupPrompt(languageName: String): String {
        return promptManager.get("prompt.cleanup").replace("{{language}}", languageName)
    }

    private suspend fun buildTranslationPrompt(targetLanguageName: String): String {
        return promptManager.get("prompt.translate").replace("{{language}}", targetLanguageName)
    }

    private suspend fun resolveDetailPrompt(
        sourceLanguage: String?,
        targetLanguage: String?,
        cleanup: Boolean
    ): Pair<String?, String?> {
        // targetLanguage is null -> cleanup only
        // targetLanguage equals sourceLanguage -> cleanup only
        // targetLanguage differs -> translate (with optional cleanup)

        if (targetLanguage == null || targetLanguage == sourceLanguage) {
            if (!cleanup) return null to sourceLanguage
            val languageName = sourceLanguage?.let { languageRepository.getLanguageName(it) }
                ?: "the same language as the input"
            return buildCleanupPrompt(languageName) to sourceLanguage
        }

        // Translation requested
        val targetLanguageName = languageRepository.getLanguageName(targetLanguage)

        if (!cleanup) {
            return buildTranslationPrompt(targetLanguageName) to targetLanguage
        }

        // Both translate and cleanup: compose prompts
        val translatePrompt = buildTranslationPrompt(targetLanguageName)
        val cleanupPrompt = buildCleanupPrompt(targetLanguageName)
        return "$translatePrompt\n$cleanupPrompt" to targetLanguage
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

    private suspend fun buildSummaryPrompt(language: String?): String {
        val basePrompt = promptManager.get("prompt.summary")
        return if (language != null) {
            val languageName = languageRepository.getLanguageName(language)
            basePrompt.replace("{{language}}", languageName)
        } else {
            basePrompt.replace("{{language}}", "the original language of the text")
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
