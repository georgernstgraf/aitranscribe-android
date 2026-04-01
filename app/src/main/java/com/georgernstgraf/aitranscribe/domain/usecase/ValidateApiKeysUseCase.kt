package com.georgernstgraf.aitranscribe.domain.usecase

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject

@ViewModelScoped
class ValidateApiKeysUseCase @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

    fun isValidKeyFormat(providerId: String, key: String): Boolean {
        if (key.length < 20) return false
        return when (providerId) {
            "groq" -> key.startsWith("gsk_")
            "openrouter" -> key.startsWith("sk-or-") || key.startsWith("sk-")
            "zai" -> key.contains(".")
            else -> key.length >= 20
        }
    }

    suspend fun validateProviderKey(providerId: String, apiKey: String): Boolean =
        when (providerId) {
            "groq" -> validateGroqKey(apiKey)
            "openrouter" -> validateOpenRouterKey(apiKey)
            "zai" -> validateZaiKey(apiKey)
            else -> true
        }

    suspend fun validateProviderModels(
        providerId: String,
        apiKey: String,
        model: String
    ): String? = when (providerId) {
        "groq" -> validateGroqModel(apiKey, model)
        "openrouter" -> validateOpenRouterModel(apiKey, model)
        "zai" -> validateZaiModel(apiKey, model)
        else -> null
    }

    suspend operator fun invoke(
        groqKey: String?,
        openRouterKey: String?
    ): ApiKeyValidationResult = withContext(Dispatchers.IO) {
        when {
            groqKey.isNullOrBlank() && openRouterKey.isNullOrBlank() -> {
                return@withContext ApiKeyValidationResult(
                    isGroqKeyValid = false,
                    isOpenRouterKeyValid = false,
                    groqKeyError = ApiKeyError.MISSING,
                    openRouterKeyError = ApiKeyError.MISSING,
                    isValid = false
                )
            }
            groqKey.isNullOrBlank() -> {
                return@withContext ApiKeyValidationResult(
                    isGroqKeyValid = false,
                    isOpenRouterKeyValid = true,
                    groqKeyError = ApiKeyError.MISSING,
                    openRouterKeyError = null,
                    isValid = false
                )
            }
            openRouterKey.isNullOrBlank() -> {
                return@withContext ApiKeyValidationResult(
                    isGroqKeyValid = true,
                    isOpenRouterKeyValid = false,
                    groqKeyError = null,
                    openRouterKeyError = ApiKeyError.MISSING,
                    isValid = false
                )
            }
            !isValidKeyFormat("groq", groqKey) -> {
                return@withContext ApiKeyValidationResult(
                    isGroqKeyValid = false,
                    isOpenRouterKeyValid = true,
                    groqKeyError = ApiKeyError.INVALID_FORMAT,
                    openRouterKeyError = null,
                    isValid = false
                )
            }
            !isValidKeyFormat("openrouter", openRouterKey) -> {
                return@withContext ApiKeyValidationResult(
                    isGroqKeyValid = true,
                    isOpenRouterKeyValid = false,
                    groqKeyError = null,
                    openRouterKeyError = ApiKeyError.INVALID_FORMAT,
                    isValid = false
                )
            }
        }

        val validatedGroqKey = groqKey ?: return@withContext ApiKeyValidationResult(
            isGroqKeyValid = false,
            isOpenRouterKeyValid = true,
            groqKeyError = ApiKeyError.MISSING,
            openRouterKeyError = null,
            isValid = false
        )
        val validatedOpenRouterKey = openRouterKey ?: return@withContext ApiKeyValidationResult(
            isGroqKeyValid = true,
            isOpenRouterKeyValid = false,
            groqKeyError = null,
            openRouterKeyError = ApiKeyError.MISSING,
            isValid = false
        )

        val groqApiReachable = validateGroqKey(validatedGroqKey)
        val openRouterApiReachable = validateOpenRouterKey(validatedOpenRouterKey)

        val groqError = if (groqApiReachable) null else ApiKeyError.API_ERROR
        val openRouterError = if (openRouterApiReachable) null else ApiKeyError.API_ERROR

        ApiKeyValidationResult(
            isGroqKeyValid = groqApiReachable,
            isOpenRouterKeyValid = openRouterApiReachable,
            groqKeyError = groqError,
            openRouterKeyError = openRouterError,
            isValid = groqApiReachable && openRouterApiReachable
        )
    }

    suspend fun validateGroqKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { false }
    }

    suspend fun validateOpenRouterKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/auth/key")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "AITranscribe/1.0")
            .get()
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { false }
    }

    private suspend fun validateZaiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.z.ai/api/paas/v4/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { false }
    }

    suspend fun validateModels(
        groqKey: String,
        openRouterKey: String,
        sttModel: String,
        llmModel: String
    ): ModelValidationResult = withContext(Dispatchers.IO) {
        val sttError = validateGroqModel(groqKey, sttModel)
        val llmError = validateOpenRouterModel(openRouterKey, llmModel)
        ModelValidationResult(
            sttModelError = sttError,
            llmModelError = llmError,
            isValid = sttError == null && llmError == null
        )
    }

    private fun validateGroqModel(apiKey: String, model: String): String? {
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Could not verify STT model (API error)"
                val body = response.body?.string() ?: return "Could not verify STT model (empty response)"
                val models = JSONObject(body).getJSONArray("data")
                val ids = (0 until models.length()).map { models.getJSONObject(it).getString("id") }
                if (model in ids) null else "Unknown STT model: $model"
            }
        }.getOrElse { "Could not verify STT model: ${it.message}" }
    }

    private fun validateOpenRouterModel(apiKey: String, model: String): String? {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "AITranscribe/1.0")
            .get()
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Could not verify LLM model (API error)"
                val body = response.body?.string() ?: return "Could not verify LLM model (empty response)"
                val models = JSONObject(body).getJSONArray("data")
                val ids = (0 until models.length()).map { models.getJSONObject(it).getString("id") }
                if (model in ids) null else "Unknown LLM model: $model"
            }
        }.getOrElse { "Could not verify LLM model: ${it.message}" }
    }

    private fun validateZaiModel(apiKey: String, model: String): String? {
        val request = Request.Builder()
            .url("https://api.z.ai/api/paas/v4/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return "Could not verify ZAI model (API error)"
                val body = response.body?.string() ?: return "Could not verify ZAI model (empty response)"
                val models = JSONObject(body).getJSONArray("data")
                val ids = (0 until models.length()).map { models.getJSONObject(it).getString("id") }
                if (model in ids) null else "Unknown ZAI model: $model"
            }
        }.getOrElse { "Could not verify ZAI model: ${it.message}" }
    }
}

data class ApiKeyValidationResult(
    val isGroqKeyValid: Boolean,
    val isOpenRouterKeyValid: Boolean,
    val groqKeyError: ApiKeyError?,
    val openRouterKeyError: ApiKeyError?,
    val isValid: Boolean
)

data class ModelValidationResult(
    val sttModelError: String?,
    val llmModelError: String?,
    val isValid: Boolean
)

enum class ApiKeyError {
    MISSING,
    INVALID_FORMAT,
    API_ERROR
}
