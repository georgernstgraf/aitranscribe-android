package com.georgernstgraf.aitranscribe.domain.usecase

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

/**
 * Use case for validating API keys.
 * Checks if both GROQ and OpenRouter API keys are set and valid.
 */
@ViewModelScoped
class ValidateApiKeysUseCase @Inject constructor(
    private val okHttpClient: OkHttpClient
) {

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
            !isValidGroqKeyFormat(groqKey) -> {
                return@withContext ApiKeyValidationResult(
                    isGroqKeyValid = false,
                    isOpenRouterKeyValid = true,
                    groqKeyError = ApiKeyError.INVALID_FORMAT,
                    openRouterKeyError = null,
                    isValid = false
                )
            }
            !isValidOpenRouterKeyFormat(openRouterKey) -> {
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

    private fun validateGroqKey(apiKey: String): Boolean {
        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .get()
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { false }
    }

    private fun validateOpenRouterKey(apiKey: String): Boolean {
        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/auth/key")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "AITranscribe/1.0")
            .get()
            .build()

        return runCatching {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { false }
    }

    private fun isValidGroqKeyFormat(key: String): Boolean {
        return key.length >= 20 && key.startsWith("gsk_")
    }

    private fun isValidOpenRouterKeyFormat(key: String): Boolean {
        return key.length >= 20 && (key.startsWith("sk-or-") || key.startsWith("sk-"))
    }
}

data class ApiKeyValidationResult(
    val isGroqKeyValid: Boolean,
    val isOpenRouterKeyValid: Boolean,
    val groqKeyError: ApiKeyError?,
    val openRouterKeyError: ApiKeyError?,
    val isValid: Boolean
)

enum class ApiKeyError {
    MISSING,
    INVALID_FORMAT,
    API_ERROR
}
