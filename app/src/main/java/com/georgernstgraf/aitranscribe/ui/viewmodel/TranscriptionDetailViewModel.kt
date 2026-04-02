package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.toDomain
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.ProviderConfig
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepository
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
import com.georgernstgraf.aitranscribe.domain.usecase.SetTranscriptionLanguageUseCase
import com.georgernstgraf.aitranscribe.util.ToastManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TranscriptionDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: TranscriptionRepository,
    private val appSettingsStore: AppSettingsStore,
    private val postProcessTextUseCase: PostProcessTextUseCase,
    private val setTranscriptionLanguageUseCase: SetTranscriptionLanguageUseCase,
    private val languageRepository: LanguageRepository,
    private val toastManager: ToastManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val transcriptionId: Long =
        checkNotNull(savedStateHandle[KEY_TRANSCRIPTION_ID]) { "Transcription ID is required" }

    private val _uiState = MutableStateFlow(TranscriptionDetailUiState())
    val uiState: StateFlow<TranscriptionDetailUiState> = _uiState.asStateFlow()

    private var suppressAutoMark = false
    private var viewFilter: ViewFilter = ViewFilter.ALL

    private val _filteredIds = MutableStateFlow<List<Long>>(emptyList())
    val filteredIds: StateFlow<List<Long>> = _filteredIds.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _activeTranscriptionId = MutableStateFlow(transcriptionId)

    init {
        savedStateHandle.get<String>(KEY_VIEW_FILTER)?.let { viewFilter = ViewFilter.valueOf(it) }
        loadFilteredIds()
        observeActiveTranscription()
        loadAvailableLanguages()
    }

    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            val activeLanguageIds = appSettingsStore.getActiveLanguages()
            val languages = if (activeLanguageIds.isEmpty()) {
                // Default to English if no languages configured
                listOf(languageRepository.getLanguageById("en")!!)
            } else {
                activeLanguageIds.mapNotNull { languageRepository.getLanguageById(it) }
            }
            _uiState.update { it.copy(availableLanguages = languages) }
        }
    }

    fun setSourceLanguage(languageId: String) {
        viewModelScope.launch {
            setTranscriptionLanguageUseCase(transcriptionId, languageId)
        }
    }

    fun cleanup() {
        viewModelScope.launch {
            val transcription = _uiState.value.transcription ?: return@launch
            val llmProvider = appSettingsStore.getLlmProvider()
            val apiKey = appSettingsStore.getActiveAuthToken(llmProvider)
            val llmModel = appSettingsStore.getProviderLlmModel(llmProvider, ProviderConfig.getDefaultLlmModel(llmProvider))

            if (apiKey.isNullOrBlank()) {
                toastManager.showToast("LLM API key is required for cleanup", isError = true)
                return@launch
            }

            _uiState.update { it.copy(isProcessing = true) }
            try {
                postProcessTextUseCase.cleanupTranscription(
                    transcriptionId = transcription.id,
                    llmModel = llmModel,
                    apiKey = apiKey,
                    llmProvider = llmProvider
                )
            } catch (e: Exception) {
                toastManager.showToast(e.message ?: "Cleanup failed", isError = true)
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun translateTo(targetLanguage: String) {
        viewModelScope.launch {
            val transcription = _uiState.value.transcription ?: return@launch
            val llmProvider = appSettingsStore.getLlmProvider()
            val apiKey = appSettingsStore.getActiveAuthToken(llmProvider)
            val llmModel = appSettingsStore.getProviderLlmModel(llmProvider, ProviderConfig.getDefaultLlmModel(llmProvider))

            if (apiKey.isNullOrBlank()) {
                toastManager.showToast("LLM API key is required for translation", isError = true)
                return@launch
            }

            _uiState.update { it.copy(isProcessing = true) }
            try {
                postProcessTextUseCase(
                    transcriptionId = transcription.id,
                    isCleanupEnabled = true,
                    targetLanguage = targetLanguage,
                    llmModel = llmModel,
                    apiKey = apiKey,
                    llmProvider = llmProvider
                )
            } catch (e: Exception) {
                toastManager.showToast(e.message ?: "Translation failed", isError = true)
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun toggleRawCleaned() {
        _uiState.update { it.copy(showRawText = !it.showRawText) }
    }

    private fun loadFilteredIds() {
        viewModelScope.launch {
            repository.getFilteredIds(viewFilter).collect { ids ->
                _filteredIds.value = ids
                val idx = ids.indexOf(transcriptionId)
                if (idx >= 0) {
                    _currentIndex.value = idx
                }
            }
        }
    }

    private fun observeActiveTranscription() {
        viewModelScope.launch {
            _activeTranscriptionId.flatMapLatest { id -> repository.getByIdFlow(id) }
                .collect { entity ->
                    if (entity != null) {
                        val transcription = entity.toDomain()
                        _uiState.update {
                            it.copy(
                                transcription = transcription,
                                isViewed = transcription.isViewed
                            )
                        }
                        if (!suppressAutoMark && !entity.seen && viewFilter != ViewFilter.UNVIEWED_ONLY) {
                            markAsViewed(transcription.id)
                        }
                    }
                }
        }
    }

    private fun markAsViewed(id: Long) {
        viewModelScope.launch { repository.markAsViewed(id) }
    }

    fun onPageChanged(index: Int) {
        val ids = _filteredIds.value
        if (ids.isEmpty()) return
        val clampedIndex = index.coerceIn(0, ids.lastIndex)
        _currentIndex.value = clampedIndex
        suppressAutoMark = false
        _activeTranscriptionId.value = ids[clampedIndex]
    }

    fun toggleViewStatus(id: Long) {
        viewModelScope.launch {
            val isCurrentlyViewed = _uiState.value.transcription?.isViewed ?: return@launch
            if (isCurrentlyViewed) {
                suppressAutoMark = true
                repository.resetViewStatus(id)
                _uiState.update { it.copy(isViewed = false) }
            } else {
                suppressAutoMark = false
                repository.markAsViewed(id)
                _uiState.update { it.copy(isViewed = true) }
            }
        }
    }

    fun shareTranscription(transcription: Transcription): Intent {
        val text = transcription.getShareText()
        val preferredApp = appSettingsStore.getPreferredShareApp()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, transcription.getShareTitle())
            if (preferredApp != null) {
                setPackage(preferredApp)
            }
        }

        return if (preferredApp != null) {
            try {
                context.packageManager.getPackageInfo(preferredApp, 0)
                intent
            } catch (_: Exception) {
                viewModelScope.launch { appSettingsStore.setPreferredShareApp(null) }
                Intent.createChooser(Intent(intent).setPackage(null), "Share transcription")
            }
        } else {
            Intent.createChooser(intent, "Share transcription")
        }
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            val ids = _filteredIds.value
            val currentIdx = ids.indexOf(id)
            
            // Determine the next ID to show
            val nextId = if (currentIdx != -1) {
                if (ids.size <= 1) null
                else if (currentIdx < ids.lastIndex) ids[currentIdx + 1]
                else ids[currentIdx - 1]
            } else null

            if (nextId == null) {
                // If this was the last item, signal UI to close
                _uiState.update { it.copy(isDeleted = true) }
                repository.deleteById(id)
            } else {
                // Proactively switch to the next item
                val nextIdx = ids.indexOf(nextId)
                _activeTranscriptionId.value = nextId
                _currentIndex.value = nextIdx
                
                // Perform deletion in background
                repository.deleteById(id)
            }
        }
    }

    companion object {
        const val KEY_TRANSCRIPTION_ID = "transcription_id"
        const val KEY_VIEW_FILTER = "view_filter"
    }
}

data class TranscriptionDetailUiState(
    val transcription: Transcription? = null,
    val isViewed: Boolean = false,
    val isDeleted: Boolean = false,
    val isProcessing: Boolean = false,
    val showRawText: Boolean = false,
    val availableLanguages: List<com.georgernstgraf.aitranscribe.domain.repository.Language> = emptyList()
)
