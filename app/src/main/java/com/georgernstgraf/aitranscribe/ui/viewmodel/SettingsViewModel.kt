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
import com.georgernstgraf.aitranscribe.data.local.ModelEntity
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
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
    private val validateApiKeysUseCase: ValidateApiKeysUseCase,
    private val providerModelDao: ProviderModelDao
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
            val models = providerModelDao.getModelsForProvider(providerId).map { it.id }
            val fallback = models.firstOrNull() ?: ProviderConfig.getSttModelsForProvider(providerId).firstOrNull() ?: ""
            val model = securePreferences.getProviderModel(providerId, fallback)
            _uiState.update { it.copy(sttProvider = providerId, sttModel = model) }
            updateDropdownModels()
        }
    }

    fun onSttModelChanged(model: String) {
        _uiState.update { it.copy(sttModel = model) }
    }

    fun onLlmProviderChanged(providerId: String) {
        viewModelScope.launch {
            val models = providerModelDao.getModelsForProvider(providerId).map { it.id }
            val fallback = models.firstOrNull() ?: ProviderConfig.getLlmModelsForProvider(providerId).firstOrNull() ?: ""
            val model = securePreferences.getProviderModel(providerId, fallback)
            _uiState.update { it.copy(llmProvider = providerId, llmModel = model) }
            updateDropdownModels()
        }
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

    fun saveProviderAuth(providerId: String, token: String) {
        viewModelScope.launch {
            securePreferences.setProviderAuthToken(providerId, token)
            // Refresh state to update auth status
            loadSettings()
        }
    }

    suspend fun getProviderToken(providerId: String): String? {
        return securePreferences.getActiveAuthToken(providerId)
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val sttProvider = securePreferences.getSttProvider()
                val llmProvider = securePreferences.getLlmProvider()
                
                // Track only providers with valid auth tokens
                val allProviderIds = ProviderConfig.allProviderIds
                val authStatus = allProviderIds.associateWith { provider ->
                    !securePreferences.getActiveAuthToken(provider).isNullOrBlank()
                }
                val activeProviders = allProviderIds.filter { provider -> authStatus[provider] == true }
                val availableProviders = allProviderIds.filter { provider -> authStatus[provider] != true }
                
                val sttModels = providerModelDao.getModelsForProvider(sttProvider)
                val sttFallback = sttModels.firstOrNull()?.id ?: ProviderConfig.getDefaultSttModel(sttProvider)
                
                val llmModels = providerModelDao.getModelsForProvider(llmProvider)
                val llmFallback = llmModels.firstOrNull()?.id ?: ProviderConfig.getDefaultLlmModel(llmProvider)

                _uiState.update {
                    SettingsUiState(
                        activeProviders = activeProviders,
                        availableProviders = availableProviders,
                        providerAuthStatus = authStatus,
                        groqApiKey = securePreferences.getGroqApiKey(),
                        openRouterApiKey = securePreferences.getProviderApiKey("openrouter"),
                        zaiApiKey = securePreferences.getProviderApiKey("zai"),
                        sttModel = securePreferences.getProviderModel(sttProvider, sttFallback),
                        sttProvider = sttProvider,
                        llmModel = securePreferences.getProviderModel(llmProvider, llmFallback),
                        llmProvider = llmProvider,
                        sttAvailableModels = sttModels,
                        llmAvailableModels = llmModels
                    )
                }
            } catch (_: Exception) {
                _uiState.update { SettingsUiState() }
            }
        }
    }

    private fun updateDropdownModels() {
        viewModelScope.launch {
            val sttModels = providerModelDao.getModelsForProvider(_uiState.value.sttProvider)
            val llmModels = providerModelDao.getModelsForProvider(_uiState.value.llmProvider)
            _uiState.update {
                it.copy(
                    sttAvailableModels = sttModels,
                    llmAvailableModels = llmModels
                )
            }
        }
    }
}

data class SettingsUiState(
    val activeProviders: List<String> = emptyList(),
    val availableProviders: List<String> = emptyList(),
    val providerAuthStatus: Map<String, Boolean> = emptyMap(),
    val sttAvailableModels: List<ModelEntity> = emptyList(),
    val llmAvailableModels: List<ModelEntity> = emptyList(),
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
