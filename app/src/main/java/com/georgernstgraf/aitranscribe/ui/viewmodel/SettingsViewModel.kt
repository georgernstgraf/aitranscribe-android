package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import com.georgernstgraf.aitranscribe.domain.model.ProviderConfig
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
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
    private val securePreferences: SecurePreferences,
    private val validateApiKeysUseCase: ValidateApiKeysUseCase,
    private val providerModelDao: ProviderModelDao,
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
            val models = providerModelDao.getModelsForProvider(providerId).map { it.id }
            val fallback = models.firstOrNull() ?: ProviderConfig.getSttModelsForProvider(providerId).firstOrNull() ?: ""
            val model = securePreferences.getProviderSttModel(providerId, fallback)
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
            val model = securePreferences.getProviderLlmModel(providerId, fallback)
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

            val activeSttToken = securePreferences.getActiveAuthToken(state.sttProvider)
            if (activeSttToken.isNullOrBlank() && state.sttProvider != "openrouter") {
                // OpenRouter allows fetching some things without auth, but STT might require it.
                // Assuming Groq and ZAI require auth for STT:
                errors.add("${state.sttProvider.replaceFirstChar { it.uppercase() }} authentication is required for STT")
            }

            val activeLlmToken = securePreferences.getActiveAuthToken(state.llmProvider)
            if (activeLlmToken.isNullOrBlank()) {
                errors.add("${state.llmProvider.replaceFirstChar { it.uppercase() }} authentication is required for LLM Post-Processing")
            }

            if (errors.isNotEmpty()) {
                _uiState.update { it.copy(isValidating = false, errorMessage = errors.joinToString("\n")) }
                return@launch
            }

            // Save provider-specific models
            securePreferences.setProviderLlmModel(state.llmProvider, state.llmModel)
            securePreferences.setProviderSttModel(state.sttProvider, state.sttModel)
            
            // Save global active providers
            securePreferences.setSttModel(state.sttModel)
            securePreferences.setLlmModel(state.llmModel)
            securePreferences.setSttProvider(state.sttProvider)
            securePreferences.setLlmProvider(state.llmProvider)

            retryQueuedTranscriptions(state.sttModel)

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
                        sttModel = securePreferences.getProviderSttModel(sttProvider, sttFallback),
                        sttProvider = sttProvider,
                        llmModel = securePreferences.getProviderLlmModel(llmProvider, llmFallback),
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

    private suspend fun retryQueuedTranscriptions(sttModel: String) {
        val queuedItems = repository.getByStatuses(
            listOf(TranscriptionStatus.STT_ERROR_PERMANENT.name)
        )
        if (queuedItems.isEmpty()) return

        for (transcription in queuedItems) {
            repository.updateSttModel(transcription.id, sttModel)
            repository.updateStatusAndError(transcription.id, TranscriptionStatus.PENDING.name, null)
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
        Log.d("SettingsViewModel", "Retrying ${queuedItems.size} queued transcription(s) with model=$sttModel")
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
