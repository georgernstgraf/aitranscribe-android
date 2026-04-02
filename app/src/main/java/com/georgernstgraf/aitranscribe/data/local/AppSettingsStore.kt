package com.georgernstgraf.aitranscribe.data.local

import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsStore @Inject constructor(
    private val appPreferencesDao: AppPreferencesDao,
    private val providerModelDao: ProviderModelDao
) {

    suspend fun setProviderAuthToken(providerId: String, token: String?) {
        providerModelDao.updateProviderApiToken(providerId, token)
    }

    suspend fun getActiveAuthToken(providerId: String): String? {
        return providerModelDao.getProviderApiToken(providerId)
    }

    suspend fun setProviderSettings(providerId: String, apiKey: String?, model: String) {
        providerModelDao.updateProviderApiToken(providerId, apiKey)
        setProviderLlmModel(providerId, model)
    }

    suspend fun getProviderLlmModel(providerId: String, defaultModel: String): String {
        return getPreference(KEY_PROVIDER_LLM_MODEL_PREFIX + providerId) ?: defaultModel
    }

    suspend fun getProviderSttModel(providerId: String, defaultModel: String): String {
        return getPreference(KEY_PROVIDER_STT_MODEL_PREFIX + providerId) ?: defaultModel
    }

    suspend fun setProviderSttModel(providerId: String, model: String) {
        setPreference(KEY_PROVIDER_STT_MODEL_PREFIX + providerId, model)
    }

    suspend fun setProviderLlmModel(providerId: String, model: String) {
        setPreference(KEY_PROVIDER_LLM_MODEL_PREFIX + providerId, model)
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

    suspend fun setLlmProvider(provider: String) {
        setPreference(KEY_LLM_PROVIDER, provider)
    }

    suspend fun getLlmProvider(): String {
        return getPreference(KEY_LLM_PROVIDER) ?: DEFAULT_LLM_PROVIDER
    }

    suspend fun setSttProvider(provider: String) {
        setPreference(KEY_STT_PROVIDER, provider)
    }

    suspend fun getSttProvider(): String {
        return getPreference(KEY_STT_PROVIDER) ?: DEFAULT_STT_PROVIDER
    }

    suspend fun setPreferredShareApp(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            appPreferencesDao.deleteByKey(KEY_PREFERRED_SHARE_APP)
        } else {
            setPreference(KEY_PREFERRED_SHARE_APP, packageName)
        }
    }

    fun getPreferredShareApp(): String? = runBlocking { getPreference(KEY_PREFERRED_SHARE_APP) }

    suspend fun clearAll() {
        appPreferencesDao.deleteAll()
        providerModelDao.clearAllProviderApiTokens()
    }

    suspend fun getActiveLanguages(): List<String> {
        return getPreference(KEY_ACTIVE_LANGUAGES)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun setActiveLanguages(languageIds: List<String>) {
        setPreference(KEY_ACTIVE_LANGUAGES, languageIds.joinToString(","))
    }

    private suspend fun setPreference(key: String, value: String) {
        appPreferencesDao.insert(
            AppPreferenceEntity(
                key = key,
                value = value,
                updatedAt = LocalDateTime.now().toString()
            )
        )
    }

    private suspend fun getPreference(key: String): String? {
        return appPreferencesDao.getByKey(key)?.value
    }

    companion object {
        private const val KEY_PROVIDER_STT_MODEL_PREFIX = "provider_stt_model_"
        private const val KEY_PROVIDER_LLM_MODEL_PREFIX = "provider_llm_model_"
        private const val KEY_STT_PROVIDER = "stt_provider"
        private const val KEY_LLM_PROVIDER = "llm_provider"
        private const val KEY_PREFERRED_SHARE_APP = "preferred_share_app"
        private const val KEY_ACTIVE_LANGUAGES = "active_languages"

        private const val DEFAULT_STT_PROVIDER = "groq"
        private const val DEFAULT_LLM_PROVIDER = "openrouter"
    }
}
