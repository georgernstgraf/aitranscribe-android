package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.Context
import android.provider.Settings as AndroidSettings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.DeleteTranscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteTranscriptionUseCase: DeleteTranscriptionUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun onGroqApiKeyChanged(apiKey: String?) {
        _uiState.update { it.copy(groqApiKey = apiKey) }
    }

    fun onOpenRouterApiKeyChanged(apiKey: String?) {
        _uiState.update { it.copy(openRouterApiKey = apiKey) }
    }

    fun onSttModelChanged(model: String) {
        _uiState.update { it.copy(sttModel = model) }
    }

    fun onLlmModelChanged(model: String) {
        _uiState.update { it.copy(llmModel = model) }
    }

    fun onDaysToDeleteChanged(days: Int) {
        _uiState.update { it.copy(daysToDelete = days) }
    }

    fun onDeleteViewFilterChanged(filter: ViewFilter) {
        _uiState.update { it.copy(deleteViewFilter = filter) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value

            saveToSecureStorage(PREF_GROQ_API_KEY, state.groqApiKey)
            saveToSecureStorage(PREF_OPENROUTER_API_KEY, state.openRouterApiKey)
            saveToSecureStorage(PREF_STT_MODEL, state.sttModel)
            saveToSecureStorage(PREF_LLM_MODEL, state.llmModel)

            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun getOldCount(daysOld: Int): Int {
        return deleteTranscriptionUseCase(
            mode = DeleteMode.OLD_ALL,
            cutoffDate = deleteTranscriptionUseCase.getCutoffDate(daysOld),
            viewFilter = _uiState.value.deleteViewFilter,
            getCount = true
        )
    }

    fun deleteOldTranscriptions() {
        viewModelScope.launch {
            val cutoffDate = deleteTranscriptionUseCase.getCutoffDate(_uiState.value.daysToDelete)
            
            val deletedCount = deleteTranscriptionUseCase(
                mode = DeleteMode.OLD_ALL,
                cutoffDate = cutoffDate,
                viewFilter = _uiState.value.deleteViewFilter,
                getCount = false
            )

            _uiState.update { it.copy(deletedCount = deletedCount) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val groqKey = loadFromSecureStorage(PREF_GROQ_API_KEY)
            val openRouterKey = loadFromSecureStorage(PREF_OPENROUTER_API_KEY)
            val sttModel = loadFromSecureStorage(PREF_STT_MODEL) ?: "whisper-large-v3-turbo"
            val llmModel = loadFromSecureStorage(PREF_LLM_MODEL) ?: "anthropic/claude-3-haiku"

            _uiState.update {
                SettingsUiState(
                    groqApiKey = groqKey,
                    openRouterApiKey = openRouterKey,
                    sttModel = sttModel,
                    llmModel = llmModel
                )
            }
        }
    }

    private fun saveToSecureStorage(key: String, value: String?) {
        if (value != null) {
            Settings.Secure.putString(context, key, value)
        }
    }

    private fun loadFromSecureStorage(key: String): String? {
        return Settings.Secure.getString(context, key)
    }

    companion object {
        const val PREF_GROQ_API_KEY = "groq_api_key"
        const val PREF_OPENROUTER_API_KEY = "openrouter_api_key"
        const val PREF_STT_MODEL = "stt_model"
        const val PREF_LLM_MODEL = "llm_model"
    }
}

data class SettingsUiState(
    val groqApiKey: String? = null,
    val openRouterApiKey: String? = null,
    val sttModel: String = "whisper-large-v3-turbo",
    val llmModel: String = "anthropic/claude-3-haiku",
    val daysToDelete: Int = 30,
    val deleteViewFilter: ViewFilter = ViewFilter.UNVIEWED_ONLY,
    val isSaved: Boolean = false,
    val deletedCount: Int? = null
)