package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TranscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadRecentTranscriptions()
    }

    fun loadRecentTranscriptions() {
        viewModelScope.launch {
            repository.getUnviewed(limit = 10).collect { transcriptions ->
                _uiState.update { it.copy(recentTranscriptions = transcriptions) }
            }
        }
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true, recordingDuration = 0) }
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false) }
    }

    fun updateRecordingDuration(seconds: Int) {
        _uiState.update { it.copy(recordingDuration = seconds) }
    }

    fun setRecordingError(error: String) {
        _uiState.update { it.copy(recordingError = error) }
    }

    fun clearRecordingError() {
        _uiState.update { it.copy(recordingError = null) }
    }
}

data class MainUiState(
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val recordingError: String? = null,
    val recentTranscriptions: List<Transcription> = emptyList()
)