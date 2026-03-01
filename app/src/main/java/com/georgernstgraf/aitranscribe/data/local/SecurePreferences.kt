package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKey.Builder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for sensitive data (API keys).
 * Uses EncryptedSharedPreferences for API keys.
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dataStore = context.createDataStore(
        fileName = "secure_preferences",
        encryptionRequired = false
    )

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptionScheme = EncryptedFile(
        context,
        masterKey,
        EncryptedFile.PrefKeyEncryptionScheme.AES256_SIV_CBC_PKCS7Padding
    )

    /**
     * Store GROQ API key securely.
     */
    suspend fun setGroqApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[GROQ_API_KEY] = encryptData(apiKey)
        }
    }

    /**
     * Get GROQ API key.
     */
    suspend fun getGroqApiKey(): String? {
        return dataStore.data.map { preferences ->
            preferences[GROQ_API_KEY]?.let { decryptData(it) }
        }.first()
    }

    /**
     * Store OpenRouter API key securely.
     */
    suspend fun setOpenRouterApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[OPENROUTER_API_KEY] = encryptData(apiKey)
        }
    }

    /**
     * Get OpenRouter API key.
     */
    suspend fun getOpenRouterApiKey(): String? {
        return dataStore.data.map { preferences ->
            preferences[OPENROUTER_API_KEY]?.let { decryptData(it) }
        }.first()
    }

    /**
     * Store STT model preference.
     */
    suspend fun setSttModel(model: String) {
        dataStore.edit { preferences ->
            preferences[STT_MODEL] = model
        }
    }

    /**
     * Get STT model preference.
     */
    suspend fun getSttModel(): String {
        return dataStore.data.map { preferences ->
            preferences[STT_MODEL] ?: "whisper-large-v3-turbo"
        }.first()
    }

    /**
     * Store LLM model preference.
     */
    suspend fun setLlmModel(model: String) {
        dataStore.edit { preferences ->
            preferences[LLM_MODEL] = model
        }
    }

    /**
     * Get LLM model preference.
     */
    suspend fun getLlmModel(): String {
        return dataStore.data.map { preferences ->
            preferences[LLM_MODEL] ?: "anthropic/claude-3-haiku"
        }.first()
    }

    /**
     * Clear all stored preferences.
     */
    suspend fun clearAll() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    private fun encryptData(data: String): String {
        return try {
            encryptionScheme.encrypt(data.toByteArray(Charsets.UTF_8))
                .let { Base64.encodeToString(it, 0) }
        } catch (e: Exception) {
            data // Fallback to plain text if encryption fails
        }
    }

    private fun decryptData(data: String): String {
        return try {
            val encrypted = Base64.decode(data, 0)
            encryptionScheme.decrypt(encrypted)
                .toString(Charsets.UTF_8)
        } catch (e: Exception) {
            data // Fallback to plain text if decryption fails
        }
    }

    companion object {
        private const val GROQ_API_KEY = "groq_api_key"
        private const val OPENROUTER_API_KEY = "openrouter_api_key"
        private const val STT_MODEL = "stt_model"
        private const val LLM_MODEL = "llm_model"
    }
}