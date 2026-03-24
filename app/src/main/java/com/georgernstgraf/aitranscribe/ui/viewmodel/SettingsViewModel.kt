package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.domain.model.DeleteMode
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.DeleteTranscriptionUseCase
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
    private val securePreferences: SecurePreferences
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

            state.groqApiKey?.let { securePreferences.setGroqApiKey(it) }
            state.openRouterApiKey?.let { securePreferences.setOpenRouterApiKey(it) }
            securePreferences.setSttModel(state.sttModel)
            securePreferences.setLlmModel(state.llmModel)

            _uiState.update { it.copy(isSaved = true) }
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

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val groqKey = securePreferences.getGroqApiKey()
                val openRouterKey = securePreferences.getOpenRouterApiKey()
                val sttModel = securePreferences.getSttModel()
                val llmModel = securePreferences.getLlmModel()

                _uiState.update {
                    SettingsUiState(
                        groqApiKey = groqKey,
                        openRouterApiKey = openRouterKey,
                        sttModel = sttModel,
                        llmModel = llmModel
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    SettingsUiState()
                }
            }
        }
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
    val deletedCount: Int? = null,
    val oldCount: Int? = null
)
