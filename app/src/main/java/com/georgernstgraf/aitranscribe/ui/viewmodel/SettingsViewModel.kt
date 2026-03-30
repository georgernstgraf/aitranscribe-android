package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import com.georgernstgraf.aitranscribe.domain.model.ProviderConfig
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.DeleteTranscriptionUseCase
import com.georgernstgraf.aitranscribe.domain.usecase.ValidateApiKeysUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val deleteTranscriptionUseCase: DeleteTranscriptionUseCase,
    private val securePreferences: SecurePreferences,
    private val validateApiKeysUseCase: ValidateApiKeysUseCase
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

    fun onZaiApiKeyChanged(apiKey: String?) {
        _uiState.update { it.copy(zaiApiKey = apiKey) }
    }

    fun onSttProviderChanged(providerId: String) {
        viewModelScope.launch {
            val model = securePreferences.getProviderModel(providerId, ProviderConfig.getSttModelsForProvider(providerId).firstOrNull() ?: "")
            _uiState.update { it.copy(sttProvider = providerId, sttModel = model) }
        }
    }

    fun onSttModelChanged(model: String) {
        _uiState.update { it.copy(sttModel = model) }
    }

    fun onLlmModelChanged(model: String) {
        _uiState.update { it.copy(llmModel = model) }
    }

    fun onLlmProviderChanged(providerId: String) {
        viewModelScope.launch {
            val apiKey = securePreferences.getProviderApiKey(providerId)
            val model = securePreferences.getProviderModel(providerId, ProviderConfig.getDefaultLlmModel(providerId))
            
            _uiState.update { it.copy(
                llmProvider = providerId,
                llmModel = model,
                openRouterApiKey = if (providerId == "openrouter") apiKey else it.openRouterApiKey,
                zaiApiKey = if (providerId == "zai") apiKey else it.zaiApiKey
            ) }
        }
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
            _uiState.update { it.copy(isValidating = true, errorMessage = null, isSaved = false) }

            val errors = mutableListOf<String>()
            if (state.groqApiKey.isNullOrBlank()) errors.add("GROQ API key is required")
            if (state.sttModel.isBlank()) errors.add("STT model cannot be empty")
            if (state.sttModel.contains(' ')) errors.add("STT model name must not contain spaces")
            if (state.llmModel.isBlank()) errors.add("LLM model cannot be empty")

            when (state.llmProvider) {
                "openrouter" -> if (state.openRouterApiKey.isNullOrBlank()) {
                    errors.add("OpenRouter API key is required when using OpenRouter")
                }
                "zai" -> if (state.zaiApiKey.isNullOrBlank()) {
                    errors.add("ZAI API key is required when using ZAI")
                } else if (!isValidZaiKeyFormat(state.zaiApiKey!!)) {
                    errors.add("ZAI API key format is invalid")
                }
            }

            if (errors.isNotEmpty()) {
                _uiState.update { it.copy(isValidating = false, errorMessage = errors.joinToString("\n")) }
                return@launch
            }

            val groqResult = validateApiKeysUseCase.validateGroqKey(state.groqApiKey!!)
            if (!groqResult) {
                _uiState.update { it.copy(isValidating = false, errorMessage = "GROQ API key validation failed") }
                return@launch
            }

            when (state.llmProvider) {
                "openrouter" -> {
                    val valid = validateApiKeysUseCase.validateOpenRouterKey(state.openRouterApiKey!!)
                    if (!valid) {
                        _uiState.update { it.copy(isValidating = false, errorMessage = "OpenRouter API key validation failed") }
                        return@launch
                    }
                }
                "zai" -> {
                    if (!isValidZaiKeyFormat(state.zaiApiKey!!)) {
                        _uiState.update { it.copy(isValidating = false, errorMessage = "ZAI API key format is invalid") }
                        return@launch
                    }
                }
            }

            state.groqApiKey?.let { securePreferences.setGroqApiKey(it) }
            
            // Save provider-specific settings
            when (state.llmProvider) {
                "openrouter" -> securePreferences.setProviderSettings("openrouter", state.openRouterApiKey, state.llmModel)
                "zai" -> securePreferences.setProviderSettings("zai", state.zaiApiKey, state.llmModel)
            }
            
            securePreferences.setSttModel(state.sttModel)
            securePreferences.setSttProvider(state.sttProvider)
            securePreferences.setLlmProvider(state.llmProvider)

            _uiState.update { it.copy(isValidating = false, isSaved = true) }
        }
    }

    private fun isValidZaiKeyFormat(key: String): Boolean {
        return key.length >= 20 && key.contains(".")
    }

    fun getOldCount(daysOld: Int) {
        viewModelScope.launch {
            val cutoffDate = deleteTranscriptionUseCase.getCutoffDate(daysOld)
            val count = deleteTranscriptionUseCase(
                mode = DeleteMode.OLD_ALL,
                cutoffDate = cutoffDate,
                viewFilter = _uiState.value.deleteViewFilter,
                getCount = true
            )
            _uiState.update { it.copy(oldCount = count) }
        }
    }

    fun deleteOldTranscriptions(daysOld: Int, viewFilter: ViewFilter) {
        viewModelScope.launch {
            val cutoffDate = deleteTranscriptionUseCase.getCutoffDate(daysOld)
            val deletedCount = deleteTranscriptionUseCase(
                mode = DeleteMode.OLD_ALL,
                cutoffDate = cutoffDate,
                viewFilter = viewFilter,
                getCount = false
            )
            _uiState.update { it.copy(deletedCount = deletedCount) }
        }
    }

    fun resetPreferredShareApp() {
        viewModelScope.launch {
            securePreferences.setPreferredShareApp(null)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val sttProvider = securePreferences.getSttProvider()
                val llmProvider = securePreferences.getLlmProvider()
                _uiState.update {
                    SettingsUiState(
                        groqApiKey = securePreferences.getGroqApiKey(),
                        openRouterApiKey = securePreferences.getProviderApiKey("openrouter"),
                        zaiApiKey = securePreferences.getProviderApiKey("zai"),
                        sttModel = securePreferences.getProviderModel(sttProvider, ProviderConfig.getDefaultSttModel(sttProvider)),
                        sttProvider = sttProvider,
                        llmModel = securePreferences.getProviderModel(llmProvider, ProviderConfig.getDefaultLlmModel(llmProvider)),
                        llmProvider = llmProvider
                    )
                }
            } catch (_: Exception) {
                _uiState.update { SettingsUiState() }
            }
        }
    }
}

data class SettingsUiState(
    val groqApiKey: String? = null,
    val openRouterApiKey: String? = null,
    val zaiApiKey: String? = null,
    val sttProvider: String = "groq",
    val sttModel: String = "whisper-large-v3-turbo",
    val llmProvider: String = "openrouter",
    val llmModel: String = "inception/mercury",
    val daysToDelete: Int = 30,
    val deleteViewFilter: ViewFilter = ViewFilter.UNVIEWED_ONLY,
    val isSaved: Boolean = false,
    val deletedCount: Int? = null,
    val oldCount: Int? = null,
    val isValidating: Boolean = false,
    val errorMessage: String? = null
)
