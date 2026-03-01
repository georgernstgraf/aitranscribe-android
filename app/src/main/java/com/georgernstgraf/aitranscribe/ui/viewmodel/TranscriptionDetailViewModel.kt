package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.toDomain
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    init {
        loadTranscription(transcriptionId)
    }

    fun loadTranscription(id: Long) {
        viewModelScope.launch {
            repository.getByIdFlow(id).collect { entity ->
                if (entity != null) {
                    val transcription = entity.toDomain()
                    
                    _uiState.update {
                        it.copy(
                            transcription = transcription,
                            isViewed = transcription.isViewed
                        )
                    }

                    markAsViewed(id)
                }
            }
        }
    }

    private fun markAsViewed(id: Long) {
        viewModelScope.launch {
            repository.markAsViewed(id)
        }
    }

    fun resetViewStatus(id: Long) {
        viewModelScope.launch {
            repository.resetViewStatus(id)
            
            _uiState.update { it.copy(isViewed = false) }
        }
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            _uiState.update { it.copy(isDeleted = true) }
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
    }
}

data class TranscriptionDetailUiState(
    val transcription: Transcription? = null,
    val isViewed: Boolean = false,
    val isDeleted: Boolean = false,
    val isCopiedToClipboard: Boolean = false
)