package com.georgernstgraf.aitranscribe.service

import android.content.Context
import android.util.Log
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
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.model.TranslationTarget
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
import com.georgernstgraf.aitranscribe.util.NetworkMonitor
import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
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
    private val openRouterApiService: OpenRouterApiService,
    private val zaiApiService: ZaiApiService,
    private val networkMonitor: NetworkMonitor,
    private val securePreferences: SecurePreferences,
    private val postProcessTextUseCase: PostProcessTextUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val queuedId = inputData.getLong(KEY_QUEUED_ID, -1)
        if (queuedId == -1L) return@withContext Result.failure()

        val queued = repository.getQueuedById(queuedId) ?: return@withContext Result.failure()
        if (!networkMonitor.isConnected()) return@withContext Result.retry()

        val transcriptionText = try {
            transcribeAudio(queued)
        } catch (e: Exception) {
            Log.e("TranscriptionWorker", "transcribeAudio failed", e)
            return@withContext Result.retry()
        }

        val processingMode = queued.postProcessingType ?: PostProcessingType.RAW.name
        val entity = TranscriptionEntity(
            originalText = transcriptionText,
            processedText = null,
            audioFilePath = queued.audioFilePath, // Keep audio file initially
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            postProcessingType = processingMode,
            status = TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = 0,
            retryCount = 0,
            summary = null
        )

        val transcriptionId = repository.insert(entity)
        repository.removeQueued(queuedId)

        val llmProvider = securePreferences.getLlmProvider()
        val llmApiKey = securePreferences.getActiveAuthToken(llmProvider)
        val llmModel = securePreferences.getLlmModel()

        val postProcessingType = when (processingMode) {
            PostProcessingType.CLEANUP.name,
            PostProcessingType.TRANSLATE_TO_EN.name,
            PostProcessingType.TRANSLATE_TO_DE.name,
            "ENGLISH" -> PostProcessingType.CLEANUP
            else -> PostProcessingType.RAW
        }

        if (!llmApiKey.isNullOrBlank()) {
            try {
                when (postProcessingType) {
                    PostProcessingType.RAW -> Unit
                    PostProcessingType.CLEANUP -> {
                        postProcessTextUseCase(
                            transcriptionId,
                            isCleanupEnabled = true,
                            translationTarget = TranslationTarget.NONE,
                            llmModel = llmModel,
                            apiKey = llmApiKey,
                            llmProvider = llmProvider
                        )
                    }
                    PostProcessingType.TRANSLATE_TO_EN -> {
                        postProcessTextUseCase(
                            transcriptionId,
                            isCleanupEnabled = true,
                            translationTarget = TranslationTarget.EN,
                            llmModel = llmModel,
                            apiKey = llmApiKey,
                            llmProvider = llmProvider
                        )
                    }
                    PostProcessingType.TRANSLATE_TO_DE -> {
                        postProcessTextUseCase(
                            transcriptionId,
                            isCleanupEnabled = true,
                            translationTarget = TranslationTarget.DE,
                            llmModel = llmModel,
                            apiKey = llmApiKey,
                            llmProvider = llmProvider
                        )
                    }
                }

                postProcessTextUseCase.generateSummary(transcriptionId, llmModel, llmApiKey, llmProvider)
                
                // Clear audio file path and delete file ONLY if post-processing succeeds
                repository.clearAudioPath(transcriptionId)
                cleanupAudioFile(queued.audioFilePath)
                
            } catch (e: Exception) {
                Log.e("TranscriptionWorker", "Post-processing failed (non-fatal)", e)
                repository.updateStatus(transcriptionId, TranscriptionStatus.COMPLETED_WITH_WARNING.name)
                repository.recordError(transcriptionId, "Post-processing skipped: ${e.message}")
            }
        } else {
            if (postProcessingType == PostProcessingType.RAW) {
                repository.clearAudioPath(transcriptionId)
                cleanupAudioFile(queued.audioFilePath)
            }
        }

        cleanupOrphanedAudioFiles()

        Result.success(workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId))
    }

    private suspend fun transcribeAudio(queued: QueuedTranscriptionEntity): String {
        val audioFile = File(queued.audioFilePath)
        if (!audioFile.exists()) throw Exception("Audio file not found: ${queued.audioFilePath}")

        val sttProvider = securePreferences.getSttProvider()
        val token = securePreferences.getActiveAuthToken(sttProvider)
        if (token.isNullOrBlank()) throw Exception("${sttProvider.replaceFirstChar { it.uppercase() }} API token not configured")
        val authorization = if (token.startsWith("Bearer ")) token else "Bearer $token"

        val response = when (sttProvider) {
            "groq" -> groqApiService.transcribeAudio(
                authorization = authorization,
                file = createAudioPart(audioFile),
                model = createModelPart(queued.sttModel),
                responseFormat = createFormatPart()
            )
            "openrouter" -> openRouterApiService.transcribeAudio(
                authorization = authorization,
                file = createAudioPart(audioFile),
                model = createModelPart(queued.sttModel),
                responseFormat = createFormatPart()
            )
            "zai" -> zaiApiService.transcribeAudio(
                authorization = authorization,
                file = createAudioPart(audioFile),
                model = createModelPart(queued.sttModel),
                responseFormat = createFormatPart()
            )
            else -> throw Exception("Unknown STT Provider: $sttProvider")
        }

        return response.body()?.text ?: throw Exception("Empty transcription response from $sttProvider (Code: ${response.code()})")
    }

    private fun createAudioPart(audioFile: File): MultipartBody.Part {
        val mediaType = "audio/mp4".toMediaType()
        val requestBody = audioFile.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
    }

    private fun createModelPart(model: String) = model.toRequestBody("text/plain".toMediaType())

    private fun createFormatPart() = "json".toRequestBody("text/plain".toMediaType())

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
                it.name.startsWith("recording_") && it.name.endsWith(".m4a") &&
                    it.absolutePath !in queuedPaths
            }?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val KEY_QUEUED_ID = "queued_id"
        const val KEY_TRANSCRIPTION_ID = "transcription_id"

        fun createInputData(queuedId: Long): Data = workDataOf(KEY_QUEUED_ID to queuedId)
    }
}
