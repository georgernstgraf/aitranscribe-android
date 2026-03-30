package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun setProviderSettings(providerId: String, apiKey: String?, model: String) {
        sharedPreferences.edit()
            .putString("${providerId}_api_key", apiKey)
            .putString("${providerId}_llm_model", model)
            .apply()
    }

    suspend fun getProviderApiKey(providerId: String): String? = sharedPreferences.getString("${providerId}_api_key", null)
    suspend fun getProviderModel(providerId: String, defaultModel: String): String = 
        sharedPreferences.getString("${providerId}_llm_model", defaultModel) ?: defaultModel

    suspend fun setGroqApiKey(apiKey: String) {
        sharedPreferences.edit().putString(GROQ_API_KEY, apiKey).apply()
    }

    suspend fun getGroqApiKey(): String? = sharedPreferences.getString(GROQ_API_KEY, null)

    suspend fun setOpenRouterApiKey(apiKey: String) {
        sharedPreferences.edit().putString(OPENROUTER_API_KEY, apiKey).apply()
    }

    suspend fun getOpenRouterApiKey(): String? = sharedPreferences.getString(OPENROUTER_API_KEY, null)

    suspend fun setZaiApiKey(apiKey: String) {
        sharedPreferences.edit().putString(ZAI_API_KEY, apiKey).apply()
    }

    suspend fun getZaiApiKey(): String? = sharedPreferences.getString(ZAI_API_KEY, null)

    suspend fun setSttModel(model: String) {
        sharedPreferences.edit().putString(STT_MODEL, model).apply()
    }

    suspend fun getSttModel(): String {
        return sharedPreferences.getString(STT_MODEL, "whisper-large-v3-turbo") ?: "whisper-large-v3-turbo"
    }

    suspend fun setLlmModel(model: String) {
        sharedPreferences.edit().putString(LLM_MODEL, model).apply()
    }

    suspend fun getLlmModel(): String {
        return sharedPreferences.getString(LLM_MODEL, "anthropic/claude-3-haiku") ?: "anthropic/claude-3-haiku"
    }

    suspend fun setLlmProvider(provider: String) {
        sharedPreferences.edit().putString(LLM_PROVIDER, provider).apply()
    }

    suspend fun getLlmProvider(): String {
        return sharedPreferences.getString(LLM_PROVIDER, "openrouter") ?: "openrouter"
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

    fun getPreferredShareApp(): String? = sharedPreferences.getString(PREFERRED_SHARE_APP, null)

    suspend fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }

    fun peekGroqApiKey(): String? = sharedPreferences.getString(GROQ_API_KEY, null)

    fun peekOpenRouterApiKey(): String? = sharedPreferences.getString(OPENROUTER_API_KEY, null)

    companion object {
        private const val GROQ_API_KEY = "groq_api_key"
        private const val OPENROUTER_API_KEY = "openrouter_api_key"
        private const val ZAI_API_KEY = "zai_api_key"
        private const val STT_MODEL = "stt_model"
        private const val LLM_MODEL = "llm_model"
        private const val LLM_PROVIDER = "llm_provider"
        private const val PROCESSING_MODE = "processing_mode"
        private const val PREFERRED_SHARE_APP = "preferred_share_app"
    }
}
