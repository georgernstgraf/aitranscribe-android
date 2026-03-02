package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError
import com.georgernstgraf.aitranscribe.domain.usecase.ValidateApiKeysUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val securePreferences: SecurePreferences,
    private val validateApiKeysUseCase: ValidateApiKeysUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onGroqApiKeyChanged(apiKey: String?) {
        _uiState.update { it.copy(groqApiKey = apiKey, groqKeyError = null, isGroqKeyValid = null) }
    }

    fun onOpenRouterApiKeyChanged(apiKey: String?) {
        _uiState.update { it.copy(openRouterApiKey = apiKey, openRouterKeyError = null, isOpenRouterKeyValid = null) }
    }

    fun validateAndSave() {
        viewModelScope.launch {
            _uiState.update { it.copy(isValidating = true, errorMessage = null) }

            val result = validateApiKeysUseCase()

            if (result.isValid) {
                saveApiKeys()
                _uiState.update { it.copy(isSetupComplete = true, isValidating = false) }
            } else {
                _uiState.update {
                    it.copy(
                        isValidating = false,
                        groqKeyError = result.groqKeyError,
                        openRouterKeyError = result.openRouterKeyError,
                        isGroqKeyValid = result.isGroqKeyValid,
                        isOpenRouterKeyValid = result.isOpenRouterKeyValid
                    )
                }
            }
        }
    }

    private suspend fun saveApiKeys() {
        _uiState.value.groqApiKey?.let { securePreferences.setGroqApiKey(it) }
        _uiState.value.openRouterApiKey?.let { securePreferences.setOpenRouterApiKey(it) }
    }
}

data class SetupUiState(
    val groqApiKey: String? = null,
    val openRouterApiKey: String? = null,
    val isGroqKeyValid: Boolean? = null,
    val isOpenRouterKeyValid: Boolean? = null,
    val groqKeyError: ApiKeyError? = null,
    val openRouterKeyError: ApiKeyError? = null,
    val isValidating: Boolean = false,
    val isSetupComplete: Boolean = false,
    val errorMessage: String? = null
)
