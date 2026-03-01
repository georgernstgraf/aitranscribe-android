package com.georgernstgraf.aitranscribe.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.util.NetworkMonitor
import com.georgernstgraf.aitranscribe.util.NotificationManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Worker for processing transcriptions in background.
 * Handles offline queue processing with notifications.
 */
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: TranscriptionRepository,
    private val groqApiService: GroqApiService,
    private val networkMonitor: NetworkMonitor,
    private val notificationManager: NotificationManager,
    private val securePreferences: SecurePreferences
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val queuedId = inputData.getLong(KEY_QUEUED_ID, -1)

        if (queuedId == -1L) {
            return@withContext Result.failure()
        }

        val queued = repository.getNextQueued()
        if (queued == null || queued.id != queuedId) {
            return@withContext Result.failure()
        }

        try {
            if (!networkMonitor.isConnected()) {
                notificationManager.showOfflineNotification()
                return@withContext Result.retry()
            }

            notificationManager.showTranscriptionProgressNotification(queuedId)

            val transcriptionText = transcribeAudio(queued)

            val entity = TranscriptionEntity(
                originalText = transcriptionText,
                processedText = null,
                audioFilePath = queued.audioFilePath,
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                postProcessingType = queued.postProcessingType,
                status = TranscriptionStatus.COMPLETED.name,
                errorMessage = null,
                playedCount = 0,
                retryCount = 0
            )

            val transcriptionId = repository.insert(entity)

            if (queued.postProcessingType != null) {
                notificationManager.showPostProcessingNotification(transcriptionId)
            } else {
                notificationManager.showTranscriptionCompleteNotification(transcriptionId)
            }

            repository.removeQueued(queuedId)

            cleanupAudioFile(queued.audioFilePath)

            Result.success(workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId))
        } catch (e: Exception) {
            notificationManager.showTranscriptionErrorNotification(
                e.message ?: "Transcription failed"
            )
            Result.retry()
        }
    }

    private suspend fun transcribeAudio(queued: QueuedTranscriptionEntity): String {
        val audioFile = File(queued.audioFilePath)
        if (!audioFile.exists()) {
            throw Exception("Audio file not found: ${queued.audioFilePath}")
        }

        val apiKey = securePreferences.getGroqApiKey()
        if (apiKey.isNullOrBlank()) {
            throw Exception("GROQ API key not configured")
        }

        return groqApiService.transcribeAudio(
            authorization = "Bearer $apiKey",
            file = createAudioPart(audioFile),
            model = createModelPart(queued.sttModel),
            responseFormat = createFormatPart()
        ).body()?.text ?: throw Exception("Empty transcription response")
    }

    private fun createAudioPart(audioFile: File): MultipartBody.Part {
        val mediaType = "audio/mpeg".toMediaType()
        val requestBody = audioFile.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
    }

    private fun createModelPart(model: String): okhttp3.RequestBody {
        return model.toRequestBody("text/plain".toMediaType())
    }

    private fun createFormatPart(): okhttp3.RequestBody {
        return "json".toRequestBody("text/plain".toMediaType())
    }

    private fun cleanupAudioFile(path: String) {
        try {
            File(path).delete()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    companion object {
        const val KEY_QUEUED_ID = "queued_id"
        const val KEY_TRANSCRIPTION_ID = "transcription_id"

        fun createInputData(queuedId: Long): Data {
            return workDataOf(KEY_QUEUED_ID to queuedId)
        }
    }
}
