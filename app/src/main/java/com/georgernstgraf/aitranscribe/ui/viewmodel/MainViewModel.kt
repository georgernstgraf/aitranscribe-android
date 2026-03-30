package com.georgernstgraf.aitranscribe.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionDao
import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.service.RecordingService
import com.georgernstgraf.aitranscribe.service.TranscriptionWorker
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
    private val queuedTranscriptionDao: QueuedTranscriptionDao,
    private val securePreferences: SecurePreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var recordingResultReceiver: RecordingResultReceiver? = null

    private var transcriptionsJob: Job? = null

    init {
        Log.e("MainViewModel", "=== INIT: MainViewModel created ===")
        Log.e("MainViewModel", "=== INIT: context=$context ===")
        loadRecentTranscriptions()
        registerRecordingResultReceiver()
        loadProcessingMode()
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
        Log.e("MainViewModel", "registerRecordingResultReceiver: START")
        recordingResultReceiver = RecordingResultReceiver()
        val filter = IntentFilter(RecordingService.ACTION_RECORDING_RESULT)
        Log.e("MainViewModel", "registerRecordingResultReceiver: filter action=${RecordingService.ACTION_RECORDING_RESULT}")
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    recordingResultReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
                Log.e("MainViewModel", "registerRecordingResultReceiver: Registered with RECEIVER_NOT_EXPORTED")
            } else {
                context.registerReceiver(recordingResultReceiver, filter)
                Log.e("MainViewModel", "registerRecordingResultReceiver: Registered (pre-Tiramisu)")
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "registerRecordingResultReceiver: FAILED", e)
        }
    }

    inner class RecordingResultReceiver : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("MainViewModel", "=== onReceive: action=${intent?.action} ===")
            if (intent?.action == RecordingService.ACTION_RECORDING_RESULT) {
                val audioPath = intent.getStringExtra(RecordingService.EXTRA_AUDIO_PATH)
                val duration = intent.getIntExtra(RecordingService.EXTRA_DURATION, 0)
                val wasCancelled = intent.getBooleanExtra(RecordingService.EXTRA_WAS_CANCELLED, false)
                
                Log.e("MainViewModel", "onReceive: audioPath=$audioPath, duration=$duration, wasCancelled=$wasCancelled")
                
                if (!wasCancelled && audioPath != null) {
                    // Start transcription worker
                    startTranscription(audioPath, duration)
                } else {
                    Log.e("MainViewModel", "onReceive: wasCancelled=$wasCancelled, audioPath=$audioPath - skipping transcription")
                }
            }
        }
    }

    private fun startTranscription(audioPath: String, duration: Int) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "startTranscription: audioPath=$audioPath, duration=$duration")
                
                // Get STT and LLM models from preferences
                val sttModel = securePreferences.getSttModel()
                val llmModel = securePreferences.getLlmModel()
                
                Log.d("MainViewModel", "startTranscription: sttModel=$sttModel, llmModel=$llmModel")
                
                // Create queued transcription entity
                val queuedTranscription = QueuedTranscriptionEntity(
                    audioFilePath = audioPath,
                    sttModel = sttModel,
                    llmModel = llmModel,
                    postProcessingType = _uiState.value.processingMode.name,
                    createdAt = LocalDateTime.now().toString(),
                    priority = 0
                )
                
                // Insert into database
                val queuedId = queuedTranscriptionDao.insert(queuedTranscription)
                Log.d("MainViewModel", "startTranscription: queuedId=$queuedId")
                
                // Start transcription worker
                val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                    .setInputData(
                        TranscriptionWorker.createInputData(queuedId = queuedId)
                    )
                    .build()
                
                WorkManager.getInstance(context).enqueue(workRequest)
                Log.d("MainViewModel", "startTranscription: WorkManager enqueued")
                
                // Refresh transcriptions list
                loadRecentTranscriptions()
            } catch (e: Exception) {
                Log.e("MainViewModel", "startTranscription: error", e)
                _uiState.update { 
                    it.copy(recordingError = "Failed to start transcription: ${e.message}")
                }
            }
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
            // Try the preferred app directly; fall back to chooser if it's gone
            try {
                context.packageManager.getPackageInfo(preferredApp, 0)
                intent
            } catch (_: Exception) {
                // App uninstalled, clear preference
                viewModelScope.launch { securePreferences.setPreferredShareApp(null) }
                Intent.createChooser(Intent(intent).setPackage(null), "Share transcription")
            }
        } else {
            Intent.createChooser(intent, "Share transcription")
        }
    }

    fun savePreferredShareApp(packageName: String?) {
        viewModelScope.launch {
            securePreferences.setPreferredShareApp(packageName)
        }
    }

    fun setViewFilter(filter: ViewFilter) {
        _uiState.update { it.copy(viewFilter = filter) }
        loadRecentTranscriptions()
    }

    fun setProcessingMode(mode: PostProcessingType) {
        _uiState.update { it.copy(processingMode = mode) }
        viewModelScope.launch {
            securePreferences.setProcessingMode(mode.name)
        }
    }

    private fun loadProcessingMode() {
        viewModelScope.launch {
            val modeName = securePreferences.getProcessingMode()
            val mode = when (modeName) {
                PostProcessingType.CLEANUP.name,
                PostProcessingType.TRANSLATE_TO_EN.name,
                PostProcessingType.TRANSLATE_TO_DE.name,
                "ENGLISH" -> PostProcessingType.CLEANUP
                else -> PostProcessingType.RAW
            }
            _uiState.update { it.copy(processingMode = mode) }
        }
    }

    private fun loadRecentTranscriptions() {
        transcriptionsJob?.cancel()
        transcriptionsJob = viewModelScope.launch {
            try {
                val flow = when (_uiState.value.viewFilter) {
                    ViewFilter.ALL -> repository.getAllTranscriptions(limit = 50)
                    ViewFilter.UNVIEWED_ONLY -> repository.getUnviewed(limit = 50)
                    ViewFilter.VIEWED -> repository.getViewed(limit = 50)
                }
                flow.collect { transcriptions ->
                    val sttProvider = securePreferences.getSttProvider()
                    val hasSttToken = !securePreferences.getActiveAuthToken(sttProvider).isNullOrBlank()
                    _uiState.update { 
                        it.copy(
                            recentTranscriptions = transcriptions,
                            isSttConfigured = hasSttToken
                        ) 
                    }
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
    val recentTranscriptions: List<Transcription> = emptyList(),
    val viewFilter: ViewFilter = ViewFilter.ALL,
    val processingMode: PostProcessingType = PostProcessingType.RAW,
    val isSttConfigured: Boolean = true
)
