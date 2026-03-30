package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.Intent
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.toDomain
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranslationTarget
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
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
    private val securePreferences: SecurePreferences,
    private val postProcessTextUseCase: PostProcessTextUseCase,
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
    }

    fun onCleanupToggled(enabled: Boolean) {
        _uiState.update { it.copy(isCleanupEnabled = enabled) }
    }

    fun translateToEnglish() {
        translate(TranslationTarget.EN)
    }

    fun translateToGerman() {
        translate(TranslationTarget.DE)
    }

    private fun translate(target: TranslationTarget) {
        viewModelScope.launch {
            val transcription = _uiState.value.transcription ?: return@launch
            val llmProvider = securePreferences.getLlmProvider()
            val apiKey = when (llmProvider) {
                "zai" -> securePreferences.getZaiApiKey()
                else -> securePreferences.getOpenRouterApiKey()
            }
            val llmModel = securePreferences.getLlmModel()

            if (apiKey.isNullOrBlank()) {
                viewModelScope.launch {
                    toastManager.showToast("LLM API key is required for translation", isError = true)
                }
                return@launch
            }

            _uiState.update { it.copy(isTranslating = true) }
            try {
                postProcessTextUseCase(
                    transcriptionId = transcription.id,
                    isCleanupEnabled = _uiState.value.isCleanupEnabled,
                    translationTarget = target,
                    llmModel = llmModel,
                    apiKey = apiKey,
                    llmProvider = llmProvider
                )
            } catch (e: Exception) {
                viewModelScope.launch {
                    toastManager.showToast(e.message ?: "Translation failed", isError = true)
                }
            } finally {
                _uiState.update { it.copy(isTranslating = false) }
            }
        }
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
                        if (!suppressAutoMark && entity.playedCount == 0 && viewFilter != ViewFilter.UNVIEWED_ONLY) {
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
        if (index in ids.indices) {
            _currentIndex.value = index
            suppressAutoMark = false
            _activeTranscriptionId.value = ids[index]
        }
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

    fun updateText(id: Long, newText: String) {
        viewModelScope.launch {
            val transcription = _uiState.value.transcription ?: return@launch
            repository.update(
                TranscriptionEntity(
                    id = transcription.id,
                    originalText = newText,
                    processedText = transcription.processedText,
                    audioFilePath = transcription.audioFilePath,
                    createdAt = transcription.createdAt.toString(),
                    postProcessingType = transcription.postProcessingType?.name,
                    status = transcription.status.name,
                    errorMessage = transcription.errorMessage,
                    playedCount = transcription.playedCount,
                    retryCount = transcription.retryCount,
                    summary = transcription.summary
                )
            )
        }
    }

    fun shareTranscription(transcription: Transcription): Intent {
        val text = transcription.getShareText()
        val preferredApp = securePreferences.getPreferredShareApp()

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
                viewModelScope.launch { securePreferences.setPreferredShareApp(null) }
                Intent.createChooser(Intent(intent).setPackage(null), "Share transcription")
            }
        } else {
            Intent.createChooser(intent, "Share transcription")
        }
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            val ids = _filteredIds.value
            val currentIdx = _currentIndex.value
            val nextId = ids.getOrNull(currentIdx + 1) ?: ids.getOrNull(currentIdx - 1)
            repository.deleteById(id)
            _uiState.update { it.copy(isDeleted = true, nextTranscriptionId = nextId) }
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
    val nextTranscriptionId: Long? = null,
    val isCleanupEnabled: Boolean = false,
    val isTranslating: Boolean = false
)
