package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive data (API keys).
 * Uses EncryptedSharedPreferences backed by Android Keystore.
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_preferences",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Store GROQ API key securely.
     */
    suspend fun setGroqApiKey(apiKey: String) {
        sharedPreferences.edit().putString(GROQ_API_KEY, apiKey).apply()
    }

    /**
     * Get GROQ API key.
     */
    suspend fun getGroqApiKey(): String? {
        return sharedPreferences.getString(GROQ_API_KEY, null)
    }

    /**
     * Store OpenRouter API key securely.
     */
    suspend fun setOpenRouterApiKey(apiKey: String) {
        sharedPreferences.edit().putString(OPENROUTER_API_KEY, apiKey).apply()
    }

    /**
     * Get OpenRouter API key.
     */
    suspend fun getOpenRouterApiKey(): String? {
        return sharedPreferences.getString(OPENROUTER_API_KEY, null)
    }

    /**
     * Store STT model preference.
     */
    suspend fun setSttModel(model: String) {
        sharedPreferences.edit().putString(STT_MODEL, model).apply()
    }

    /**
     * Get STT model preference.
     */
    suspend fun getSttModel(): String {
        return sharedPreferences.getString(STT_MODEL, "whisper-large-v3-turbo") ?: "whisper-large-v3-turbo"
    }

    /**
     * Store LLM model preference.
     */
    suspend fun setLlmModel(model: String) {
        sharedPreferences.edit().putString(LLM_MODEL, model).apply()
    }

    /**
     * Get LLM model preference.
     */
    suspend fun getLlmModel(): String {
        return sharedPreferences.getString(LLM_MODEL, "anthropic/claude-3-haiku") ?: "anthropic/claude-3-haiku"
    }

    suspend fun setProcessingMode(mode: String) {
        sharedPreferences.edit().putString(PROCESSING_MODE, mode).apply()
    }

    suspend fun getProcessingMode(): String {
        return sharedPreferences.getString(PROCESSING_MODE, "RAW") ?: "RAW"
    }

    suspend fun setPreferredShareApp(packageName: String?) {
        sharedPreferences.edit().putString(PREFERRED_SHARE_APP, packageName).apply()
    }

    fun getPreferredShareApp(): String? {
        return sharedPreferences.getString(PREFERRED_SHARE_APP, null)
    }

    suspend fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun peekGroqApiKey(): String? = sharedPreferences.getString(GROQ_API_KEY, null)

    fun peekOpenRouterApiKey(): String? = sharedPreferences.getString(OPENROUTER_API_KEY, null)

    companion object {
        private const val GROQ_API_KEY = "groq_api_key"
        private const val OPENROUTER_API_KEY = "openrouter_api_key"
        private const val STT_MODEL = "stt_model"
        private const val LLM_MODEL = "llm_model"
        private const val PROCESSING_MODE = "processing_mode"
        private const val PREFERRED_SHARE_APP = "preferred_share_app"
    }
}
