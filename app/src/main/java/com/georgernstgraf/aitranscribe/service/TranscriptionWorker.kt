package com.georgernstgraf.aitranscribe.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
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

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: TranscriptionRepository,
    private val groqApiService: GroqApiService,
    private val networkMonitor: NetworkMonitor,
    private val notificationManager: NotificationManager,
    private val securePreferences: SecurePreferences,
    private val postProcessTextUseCase: PostProcessTextUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val queuedId = inputData.getLong(KEY_QUEUED_ID, -1)
        Log.d("TranscriptionWorker", "doWork: queuedId=$queuedId")

        if (queuedId == -1L) {
            Log.e("TranscriptionWorker", "doWork: FAILED - invalid queuedId")
            return@withContext Result.failure()
        }

        val queued = repository.getQueuedById(queuedId)
        if (queued == null) {
            Log.e("TranscriptionWorker", "doWork: FAILED - queued not found with id=$queuedId")
            return@withContext Result.failure()
        }

        if (!networkMonitor.isConnected()) {
            notificationManager.showOfflineNotification()
            return@withContext Result.retry()
        }

        notificationManager.showTranscriptionProgressNotification(queuedId)

        val transcriptionText = try {
            transcribeAudio(queued)
        } catch (e: Exception) {
            Log.e("TranscriptionWorker", "transcribeAudio failed", e)
            notificationManager.showTranscriptionErrorNotification(
                e.message ?: "Transcription failed"
            )
            return@withContext Result.retry()
        }
        Log.d("TranscriptionWorker", "doWork: transcriptionText='$transcriptionText', length=${transcriptionText.length}")

        val processingMode = queued.postProcessingType ?: PostProcessingType.RAW.name

        val entity = TranscriptionEntity(
            originalText = transcriptionText,
            processedText = null,
            audioFilePath = null,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            postProcessingType = processingMode,
            status = TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = 0,
            retryCount = 0,
            summary = null
        )

        val transcriptionId = repository.insert(entity)
        Log.d("TranscriptionWorker", "doWork: Saved transcription with id=$transcriptionId")

        repository.removeQueued(queuedId)
        cleanupAudioFile(queued.audioFilePath)
        cleanupOrphanedAudioFiles()

        val mode = try {
            PostProcessingType.valueOf(processingMode)
        } catch (_: Exception) {
            PostProcessingType.RAW
        }

        try {
            if (mode != PostProcessingType.RAW) {
                val openRouterKey = securePreferences.getOpenRouterApiKey()
                val llmModel = securePreferences.getLlmModel()
                if (!openRouterKey.isNullOrBlank()) {
                    notificationManager.showPostProcessingNotification(transcriptionId)
                    postProcessTextUseCase(transcriptionId, mode, llmModel, openRouterKey)
                }
            }

            val openRouterKey = securePreferences.getOpenRouterApiKey()
            if (!openRouterKey.isNullOrBlank()) {
                val llmModel = securePreferences.getLlmModel()
                postProcessTextUseCase.generateSummary(transcriptionId, llmModel, openRouterKey)
            }
        } catch (e: Exception) {
            Log.e("TranscriptionWorker", "Post-processing failed (non-fatal)", e)
            val errorMsg = e.message?.take(100) ?: "Post-processing failed"
            notificationManager.showTranscriptionErrorNotification(
                "Post-processing: $errorMsg"
            )
        }

        notificationManager.showTranscriptionCompleteNotification(transcriptionId)

        Result.success(workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId))
    }

    private suspend fun transcribeAudio(queued: com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity): String {
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
        val mediaType = "audio/mp4".toMediaType()
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
        } catch (_: Exception) {
        }
    }

    private suspend fun cleanupOrphanedAudioFiles() {
        try {
            val queuedPaths = repository.getQueuedAudioPaths().toSet()
            context.cacheDir.listFiles()?.filter {
                it.name.startsWith("recording_") && it.name.endsWith(".m4a")
                        && it.absolutePath !in queuedPaths
            }?.forEach { it.delete() }
        } catch (_: Exception) {
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
