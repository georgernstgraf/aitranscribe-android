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
    @ApplicationContext private val context: Context,
    private val providerModelDao: ProviderModelDao
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

    suspend fun setProviderAuthToken(providerId: String, token: String?) {
        providerModelDao.updateProviderApiToken(providerId, token)
    }

    suspend fun getActiveAuthToken(providerId: String): String? {
        return providerModelDao.getProviderApiToken(providerId)
    }

    suspend fun setProviderSettings(providerId: String, apiKey: String?, model: String) {
        providerModelDao.updateProviderApiToken(providerId, apiKey)
        sharedPreferences.edit().putString("${providerId}_llm_model", model).apply()
    }

    suspend fun getProviderLlmModel(providerId: String, defaultModel: String): String = 
        sharedPreferences.getString("${providerId}_llm_model", defaultModel) ?: defaultModel

    suspend fun getProviderSttModel(providerId: String, defaultModel: String): String = 
        sharedPreferences.getString("${providerId}_stt_model", defaultModel) ?: defaultModel

    suspend fun setProviderSttModel(providerId: String, model: String) {
        sharedPreferences.edit().putString("${providerId}_stt_model", model).apply()
    }

    suspend fun setProviderLlmModel(providerId: String, model: String) {
        sharedPreferences.edit().putString("${providerId}_llm_model", model).apply()
    }

    suspend fun setGroqApiKey(apiKey: String) {
        providerModelDao.updateProviderApiToken("groq", apiKey)
    }

    suspend fun getGroqApiKey(): String? = providerModelDao.getProviderApiToken("groq")

    suspend fun setOpenRouterApiKey(apiKey: String) {
        providerModelDao.updateProviderApiToken("openrouter", apiKey)
    }

    suspend fun getOpenRouterApiKey(): String? = providerModelDao.getProviderApiToken("openrouter")

    suspend fun setZaiApiKey(apiKey: String) {
        providerModelDao.updateProviderApiToken("zai", apiKey)
    }

    suspend fun getZaiApiKey(): String? = providerModelDao.getProviderApiToken("zai")

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

    suspend fun setSttProvider(provider: String) {
        sharedPreferences.edit().putString(STT_PROVIDER, provider).apply()
    }

    suspend fun getSttProvider(): String {
        return sharedPreferences.getString(STT_PROVIDER, "groq") ?: "groq"
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

    companion object {
        private const val STT_MODEL = "stt_model"
        private const val STT_PROVIDER = "stt_provider"
        private const val LLM_MODEL = "llm_model"
        private const val LLM_PROVIDER = "llm_provider"
        private const val PROCESSING_MODE = "processing_mode"
        private const val PREFERRED_SHARE_APP = "preferred_share_app"
    }
}
