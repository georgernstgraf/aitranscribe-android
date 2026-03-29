package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.toDomain
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
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
        val filterName = savedStateHandle.get<String>(KEY_VIEW_FILTER)
        if (filterName != null) {
            viewFilter = ViewFilter.valueOf(filterName)
        }
        loadFilteredIds()
        observeActiveTranscription()
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
            _activeTranscriptionId.flatMapLatest { id ->
                repository.getByIdFlow(id)
            }.collect { entity ->
                if (entity != null) {
                    val transcription = entity.toDomain()
                    _uiState.update {
                        it.copy(
                            transcription = transcription,
                            isViewed = transcription.isViewed
                        )
                    }
                    if (!suppressAutoMark) {
                        markAsViewed(transcription.id)
                    }
                }
            }
        }
    }

    private fun markAsViewed(id: Long) {
        viewModelScope.launch {
            repository.markAsViewed(id)
        }
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

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            val ids = _filteredIds.value
            val currentIdx = _currentIndex.value
            // Pick next or prev as fallback
            val nextId = ids.getOrNull(currentIdx + 1) ?: ids.getOrNull(currentIdx - 1)
            repository.deleteById(id)
            _uiState.update { it.copy(isDeleted = true, nextTranscriptionId = nextId) }
        }
    }

    fun copyToClipboard() {
        viewModelScope.launch {
            val text = _uiState.value.transcription?.getDisplayText() ?: return@launch

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("transcription", text)
            clipboard.setPrimaryClip(clip)

            _uiState.update { it.copy(isCopiedToClipboard = true) }

            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(isCopiedToClipboard = false) }
        }
    }

    private fun Transcription.getDisplayText(): String {
        return processedText ?: originalText
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
    val isCopiedToClipboard: Boolean = false,
    val nextTranscriptionId: Long? = null
)
