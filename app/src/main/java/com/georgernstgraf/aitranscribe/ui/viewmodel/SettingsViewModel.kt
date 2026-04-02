package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import com.georgernstgraf.aitranscribe.domain.model.ProviderConfig
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.repository.Language
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepository
import com.georgernstgraf.aitranscribe.domain.usecase.DeleteTranscriptionUseCase
import com.georgernstgraf.aitranscribe.domain.usecase.ValidateApiKeysUseCase
import com.georgernstgraf.aitranscribe.service.TranscriptionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import com.georgernstgraf.aitranscribe.data.local.ModelEntity
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
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
    private val repository: TranscriptionRepository,
    private val appSettingsStore: AppSettingsStore,
    private val validateApiKeysUseCase: ValidateApiKeysUseCase,
    private val providerModelDao: ProviderModelDao,
    private val languageRepository: LanguageRepository,
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

    fun onZaiApiKeyChanged(apiKey: String?) {
        _uiState.update { it.copy(zaiApiKey = apiKey) }
    }

    fun onSttProviderChanged(providerId: String) {
        viewModelScope.launch {
            val models = providerModelDao.getModelsForProvider(providerId).map { it.externalId }
            val fallback = models.firstOrNull() ?: ProviderConfig.getSttModelsForProvider(providerId).firstOrNull() ?: ""
            val model = appSettingsStore.getProviderSttModel(providerId, fallback)
            _uiState.update { it.copy(sttProvider = providerId, sttModel = model) }
            updateDropdownModels()
        }
    }

    fun onSttModelChanged(model: String) {
        _uiState.update { it.copy(sttModel = model) }
    }

    fun onLlmProviderChanged(providerId: String) {
        viewModelScope.launch {
            val models = providerModelDao.getModelsForProvider(providerId).map { it.externalId }
            val fallback = models.firstOrNull() ?: ProviderConfig.getLlmModelsForProvider(providerId).firstOrNull() ?: ""
            val model = appSettingsStore.getProviderLlmModel(providerId, fallback)
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
            if (state.sttModel.isBlank()) errors.add("STT model cannot be empty")
            if (state.sttModel.contains(' ')) errors.add("STT model name must not contain spaces")
            if (state.llmModel.isBlank()) errors.add("LLM model cannot be empty")

            val activeSttToken = getEffectiveProviderToken(state.sttProvider)
            if (activeSttToken.isNullOrBlank() && state.sttProvider != "openrouter") {
                errors.add("${state.sttProvider.replaceFirstChar { it.uppercase() }} authentication is required for STT")
            }

            val activeLlmToken = getEffectiveProviderToken(state.llmProvider)
            if (activeLlmToken.isNullOrBlank()) {
                errors.add("${state.llmProvider.replaceFirstChar { it.uppercase() }} authentication is required for LLM Post-Processing")
            }

            if (errors.isNotEmpty()) {
                _uiState.update { it.copy(isValidating = false, errorMessage = errors.joinToString("\n")) }
                return@launch
            }

            val onlineErrors = mutableListOf<String>()

            val sttToken = activeSttToken!!
            if (!validateApiKeysUseCase.isValidKeyFormat(state.sttProvider, sttToken)) {
                onlineErrors.add("${state.sttProvider.replaceFirstChar { it.uppercase() }} API key has invalid format")
            } else {
                val sttReachable = validateApiKeysUseCase.validateProviderKey(state.sttProvider, sttToken)
                if (!sttReachable) {
                    onlineErrors.add("${state.sttProvider.replaceFirstChar { it.uppercase() }} API key verification failed")
                }
            }

            val llmToken = activeLlmToken!!
            if (!validateApiKeysUseCase.isValidKeyFormat(state.llmProvider, llmToken)) {
                onlineErrors.add("${state.llmProvider.replaceFirstChar { it.uppercase() }} API key has invalid format")
            } else {
                val llmReachable = validateApiKeysUseCase.validateProviderKey(state.llmProvider, llmToken)
                if (!llmReachable) {
                    onlineErrors.add("${state.llmProvider.replaceFirstChar { it.uppercase() }} API key verification failed")
                }
            }

            if (onlineErrors.isNotEmpty()) {
                _uiState.update { it.copy(isValidating = false, errorMessage = onlineErrors.joinToString("\n")) }
                return@launch
            }

            appSettingsStore.setProviderLlmModel(state.llmProvider, state.llmModel)
            appSettingsStore.setProviderSttModel(state.sttProvider, state.sttModel)
            appSettingsStore.setSttProvider(state.sttProvider)
            appSettingsStore.setLlmProvider(state.llmProvider)

            retryQueuedTranscriptions()

            _uiState.update { it.copy(isValidating = false, isSaved = true) }
        }
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
            appSettingsStore.setProviderAuthToken(providerId, token)
            loadSettings()
        }
    }

    fun disconnectProvider(providerId: String) {
        viewModelScope.launch {
            appSettingsStore.setProviderAuthToken(providerId, null)
            loadSettings()
        }
    }

    suspend fun getProviderToken(providerId: String): String? {
        return getEffectiveProviderToken(providerId)
    }

    suspend fun validateAndSaveProviderAuth(providerId: String, key: String): ProviderAuthResult {
        if (key.isBlank()) {
            return ProviderAuthResult.Error("API key cannot be empty")
        }
        if (!validateApiKeysUseCase.isValidKeyFormat(providerId, key)) {
            val formatHint = when (providerId) {
                "groq" -> "Groq keys start with 'gsk_'"
                "openrouter" -> "OpenRouter keys start with 'sk-or-' or 'sk-'"
                "zai" -> "ZAI keys are in hex.base64 format"
                else -> "Invalid key format"
            }
            return ProviderAuthResult.Error("Invalid key format. $formatHint")
        }
        val onlineValid = validateApiKeysUseCase.validateProviderKey(providerId, key)
        if (!onlineValid) {
            return ProviderAuthResult.Error("Key verification failed — check your key and network connection")
        }
        appSettingsStore.setProviderAuthToken(providerId, key)
        loadSettings()
        return ProviderAuthResult.Success
    }

    fun loadSettings() {
        viewModelScope.launch {
            try {
                val sttProvider = appSettingsStore.getSttProvider()
                val llmProvider = appSettingsStore.getLlmProvider()
                
                // Track only providers with valid auth tokens
                val allProviderIds = ProviderConfig.allProviderIds
                val authStatus = allProviderIds.associateWith { provider ->
                    !getEffectiveProviderToken(provider).isNullOrBlank()
                }
                val activeProviders = allProviderIds.filter { provider -> authStatus[provider] == true }
                val availableProviders = allProviderIds.filter { provider -> authStatus[provider] != true }
                
                val sttModels = providerModelDao.getModelsForProvider(sttProvider)
                val sttFallback = sttModels.firstOrNull()?.externalId ?: ProviderConfig.getDefaultSttModel(sttProvider)
                
                val llmModels = providerModelDao.getModelsForProvider(llmProvider)
                val llmFallback = llmModels.firstOrNull()?.externalId ?: ProviderConfig.getDefaultLlmModel(llmProvider)

                _uiState.update {
                    SettingsUiState(
                        activeProviders = activeProviders,
                        availableProviders = availableProviders,
                        providerAuthStatus = authStatus,
                        groqApiKey = appSettingsStore.getActiveAuthToken("groq"),
                        openRouterApiKey = appSettingsStore.getActiveAuthToken("openrouter"),
                        zaiApiKey = appSettingsStore.getActiveAuthToken("zai"),
                        sttModel = appSettingsStore.getProviderSttModel(sttProvider, sttFallback),
                        sttProvider = sttProvider,
                        llmModel = appSettingsStore.getProviderLlmModel(llmProvider, llmFallback),
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

    private suspend fun getEffectiveProviderToken(providerId: String): String? {
        return appSettingsStore.getActiveAuthToken(providerId)
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

    private suspend fun retryQueuedTranscriptions() {
        val queuedItems = repository.getUnfinishedSttTranscriptions()
        if (queuedItems.isEmpty()) return

        for (transcription in queuedItems) {
            val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(TranscriptionWorker.createInputData(transcriptionId = transcription.id))
                .build()
            WorkManager.getInstance(context)
                .beginUniqueWork(
                    "transcription_${transcription.id}",
                    ExistingWorkPolicy.KEEP,
                    workRequest
                )
                .enqueue()
        }
        Log.d("SettingsViewModel", "Retrying ${queuedItems.size} queued transcription(s)")
    }

    // Language management
    fun loadLanguages() {
        viewModelScope.launch {
            languageRepository.getAllLanguages().collect { languages ->
                val activeCount = languages.count { it.isActive }
                _uiState.update {
                    it.copy(
                        allLanguages = languages,
                        activeLanguageCount = activeCount
                    )
                }
            }
        }
    }

    suspend fun toggleLanguageActive(id: String, isActive: Boolean): Boolean {
        val activeCount = languageRepository.getActiveLanguageCount()
        // Prevent unchecking the last active language
        if (!isActive && activeCount <= 1) {
            return false
        }
        languageRepository.setLanguageActive(id, isActive)
        return true
    }

    fun getLanguageDisplayName(language: Language): String {
        return if (language.nativeName != null && language.nativeName != language.name) {
            "${language.name} (${language.nativeName})"
        } else {
            language.name
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
    val errorMessage: String? = null,
    val allLanguages: List<Language> = emptyList(),
    val activeLanguageCount: Int = 0
)

sealed class ProviderAuthResult {
    data object Success : ProviderAuthResult()
    data class Error(val message: String) : ProviderAuthResult()
}
