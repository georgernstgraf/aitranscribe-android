package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Use case for validating API keys.
 * Checks if both GROQ and OpenRouter API keys are set and valid.
 */
@ViewModelScoped
class ValidateApiKeysUseCase @Inject constructor(
    private val securePreferences: SecurePreferences
) {

    suspend operator fun invoke(): ApiKeyValidationResult {
        val groqKey = securePreferences.getGroqApiKey()
        val openRouterKey = securePreferences.getOpenRouterApiKey()

        return when {
            groqKey.isNullOrBlank() && openRouterKey.isNullOrBlank() -> {
                ApiKeyValidationResult(
                    isGroqKeyValid = false,
                    isOpenRouterKeyValid = false,
                    groqKeyError = ApiKeyError.MISSING,
                    openRouterKeyError = ApiKeyError.MISSING,
                    isValid = false
                )
            }
            groqKey.isNullOrBlank() -> {
                ApiKeyValidationResult(
                    isGroqKeyValid = false,
                    isOpenRouterKeyValid = true,
                    groqKeyError = ApiKeyError.MISSING,
                    openRouterKeyError = null,
                    isValid = false
                )
            }
            openRouterKey.isNullOrBlank() -> {
                ApiKeyValidationResult(
                    isGroqKeyValid = true,
                    isOpenRouterKeyValid = false,
                    groqKeyError = null,
                    openRouterKeyError = ApiKeyError.MISSING,
                    isValid = false
                )
            }
            !isValidGroqKeyFormat(groqKey) -> {
                ApiKeyValidationResult(
                    isGroqKeyValid = false,
                    isOpenRouterKeyValid = true,
                    groqKeyError = ApiKeyError.INVALID_FORMAT,
                    openRouterKeyError = null,
                    isValid = false
                )
            }
            !isValidOpenRouterKeyFormat(openRouterKey) -> {
                ApiKeyValidationResult(
                    isGroqKeyValid = true,
                    isOpenRouterKeyValid = false,
                    groqKeyError = null,
                    openRouterKeyError = ApiKeyError.INVALID_FORMAT,
                    isValid = false
                )
            }
            else -> {
                ApiKeyValidationResult(
                    isGroqKeyValid = true,
                    isOpenRouterKeyValid = true,
                    groqKeyError = null,
                    openRouterKeyError = null,
                    isValid = true
                )
            }
        }
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
