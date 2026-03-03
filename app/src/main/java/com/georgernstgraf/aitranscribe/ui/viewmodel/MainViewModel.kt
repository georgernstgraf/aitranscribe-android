package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: TranscriptionRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var recordingResultReceiver: RecordingResultReceiver? = null

    init {
        loadRecentTranscriptions()
        registerRecordingResultReceiver()
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        recordingResultReceiver?.let {
            context.unregisterReceiver(it)
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = true, recordingDuration = 0, recordingError = null) }
                
                // Start recording service
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START_RECORDING
                }
                context.startService(intent)
                
                // Start timer to update duration
                startRecordingTimer()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isRecording = false,
                        recordingError = "Failed to start recording: ${e.message}"
                    )
                }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRecording = false) }
                
                // Stop recording service
                val intent = Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP_RECORDING
                }
                context.startService(intent)
                
                // Stop timer
                recordingTimerJob?.cancel()
                recordingTimerJob = null
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isRecording = false,
                        recordingError = "Failed to stop recording: ${e.message}"
                    )
                }
            }
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // Update every second
                _uiState.update { it.copy(recordingDuration = it.recordingDuration + 1) }
            }
        }
    }

    private fun registerRecordingResultReceiver() {
        recordingResultReceiver = RecordingResultReceiver()
        val filter = IntentFilter(RecordingService.ACTION_RECORDING_RESULT)
        context.registerReceiver(recordingResultReceiver, filter)
    }

    inner class RecordingResultReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == RecordingService.ACTION_RECORDING_RESULT) {
                val audioPath = intent.getStringExtra(RecordingService.EXTRA_AUDIO_PATH)
                val duration = intent.getIntExtra(RecordingService.EXTRA_DURATION, 0)
                val wasCancelled = intent.getBooleanExtra(RecordingService.EXTRA_WAS_CANCELLED, false)
                
                // Handle the result - you could trigger transcription here
                // For now just log it
                if (!wasCancelled && audioPath != null) {
                    // TODO: Start transcription process
                }
            }
        }
    }

    private fun loadRecentTranscriptions() {
        viewModelScope.launch {
            try {
                repository.getUnviewed(limit = 10).collect { transcriptions ->
                    _uiState.update { it.copy(recentTranscriptions = transcriptions) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(recentTranscriptions = emptyList()) }
            }
        }
    }
}

data class MainUiState(
    val isRecording: Boolean = false,
    val recordingDuration: Int = 0,
    val recordingError: String? = null,
    val recentTranscriptions: List<Transcription> = emptyList()
)
