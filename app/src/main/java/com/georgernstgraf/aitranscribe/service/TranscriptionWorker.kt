package com.georgernstgraf.aitranscribe.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
import com.georgernstgraf.aitranscribe.data.local.SecurePreferences
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
    private val providerModelDao: ProviderModelDao,
    private val postProcessTextUseCase: PostProcessTextUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val transcriptionId = inputData.getLong(KEY_TRANSCRIPTION_ID, -1)
        if (transcriptionId == -1L) return@withContext Result.failure()

        val transcription = repository.getById(transcriptionId) ?: return@withContext Result.failure()
        val audioPath = transcription.audioFilePath ?: return@withContext Result.failure()

        if (!networkMonitor.isConnected()) {
            repository.updateStatusAndError(
                transcriptionId,
                TranscriptionStatus.NO_NETWORK.name,
                "No network available"
            )
            return@withContext Result.failure()
        }

        repository.updateStatusAndError(transcriptionId, TranscriptionStatus.PROCESSING.name, null)

        val sttModel = transcription.sttModel ?: securePreferences.getSttModel()
        val llmProvider = securePreferences.getLlmProvider()
        val llmModel = securePreferences.getProviderLlmModel(
            llmProvider,
            transcription.llmModel ?: securePreferences.getLlmModel()
        )
        val processingMode = transcription.postProcessingType ?: PostProcessingType.RAW.name

        val transcriptionText = try {
            transcribeAudio(audioPath, sttModel)
        } catch (e: SttPermanentException) {
            Log.e("TranscriptionWorker", "Permanent STT failure", e)
            repository.updateStatusAndError(
                transcriptionId,
                TranscriptionStatus.STT_ERROR_PERMANENT.name,
                e.message
            )
            return@withContext Result.failure()
        } catch (e: Exception) {
            Log.e("TranscriptionWorker", "Retryable STT failure", e)
            repository.updateStatusAndError(
                transcriptionId,
                TranscriptionStatus.STT_ERROR_RETRYABLE.name,
                e.message
            )
            return@withContext Result.failure()
        }

        repository.update(
            transcription.copy(
                originalText = transcriptionText,
                processedText = null,
                status = TranscriptionStatus.COMPLETED.name,
                errorMessage = null,
                sttModel = sttModel,
                llmModel = llmModel,
                postProcessingType = processingMode
            )
        )

        val llmApiKey = securePreferences.getActiveAuthToken(llmProvider)

        val postProcessingType = when (processingMode) {
            PostProcessingType.CLEANUP.name,
            PostProcessingType.TRANSLATE_TO_EN.name,
            PostProcessingType.TRANSLATE_TO_DE.name,
            "ENGLISH" -> PostProcessingType.CLEANUP
            else -> PostProcessingType.RAW
        }

        if (!llmApiKey.isNullOrBlank()) {
            try {
                performPostProcessing(
                    transcriptionId = transcriptionId,
                    postProcessingType = postProcessingType,
                    llmModel = llmModel,
                    llmApiKey = llmApiKey,
                    llmProvider = llmProvider
                )

                // Clear audio file path and delete file ONLY if post-processing succeeds
                repository.clearAudioPath(transcriptionId)
                cleanupAudioFile(audioPath)

            } catch (e: Exception) {
                if (llmProvider == "zai") {
                    val fallbackModel = resolveZaiFallbackModel(llmModel)
                    if (!fallbackModel.isNullOrBlank() && fallbackModel != llmModel) {
                        try {
                            Log.w("TranscriptionWorker", "Retrying ZAI post-processing with fallback model=$fallbackModel")
                            performPostProcessing(
                                transcriptionId = transcriptionId,
                                postProcessingType = postProcessingType,
                                llmModel = fallbackModel,
                                llmApiKey = llmApiKey,
                                llmProvider = llmProvider
                            )
                            securePreferences.setProviderLlmModel("zai", fallbackModel)
                            securePreferences.setLlmModel(fallbackModel)
                            repository.clearAudioPath(transcriptionId)
                            cleanupAudioFile(audioPath)
                            cleanupOrphanedAudioFiles()
                            return@withContext Result.success(workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId))
                        } catch (retryError: Exception) {
                            Log.e("TranscriptionWorker", "ZAI fallback post-processing failed (non-fatal)", retryError)
                        }
                    }
                }
                Log.e("TranscriptionWorker", "Post-processing failed (non-fatal)", e)
                repository.updateStatus(transcriptionId, TranscriptionStatus.COMPLETED_WITH_WARNING.name)
                repository.recordError(transcriptionId, "Post-processing skipped: ${e.message}")
            }
        } else {
            if (postProcessingType == PostProcessingType.RAW) {
                repository.clearAudioPath(transcriptionId)
                cleanupAudioFile(audioPath)
            }
        }

        cleanupOrphanedAudioFiles()

        Result.success(workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId))
    }

    private suspend fun performPostProcessing(
        transcriptionId: Long,
        postProcessingType: PostProcessingType,
        llmModel: String,
        llmApiKey: String,
        llmProvider: String
    ) {
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
    }

    private suspend fun resolveZaiFallbackModel(currentModel: String): String? {
        val dynamic = providerModelDao.getModelsForProvider("zai").map { it.externalId }
        val ordered = dynamic.sortedWith(
            compareByDescending<String> { it.contains("4.5-flash", ignoreCase = true) }
                .thenByDescending { it.contains("flash", ignoreCase = true) }
        )
        return ordered.firstOrNull { it != currentModel }
    }

    private suspend fun transcribeAudio(audioPath: String, sttModel: String): String {
        val audioFile = File(audioPath)
        if (!audioFile.exists()) throw Exception("Audio file not found: $audioPath")

        val sttProvider = securePreferences.getSttProvider()
        val token = securePreferences.getActiveAuthToken(sttProvider)
        if (token.isNullOrBlank()) throw Exception("${sttProvider.replaceFirstChar { it.uppercase() }} API token not configured")
        val authorization = if (token.startsWith("Bearer ")) token else "Bearer $token"

        val response = when (sttProvider) {
            "groq" -> groqApiService.transcribeAudio(
                authorization = authorization,
                file = createAudioPart(audioFile),
                model = createModelPart(sttModel),
                responseFormat = createFormatPart()
            )
            "openrouter" -> openRouterApiService.transcribeAudio(
                authorization = authorization,
                file = createAudioPart(audioFile),
                model = createModelPart(sttModel),
                responseFormat = createFormatPart()
            )
            "zai" -> zaiApiService.transcribeAudio(
                authorization = authorization,
                file = createAudioPart(audioFile),
                model = createModelPart(sttModel),
                responseFormat = createFormatPart()
            )
            else -> throw Exception("Unknown STT Provider: $sttProvider")
        }

        if (!response.isSuccessful) {
            val code = response.code()
            val errorText = runCatching { response.errorBody()?.string() }.getOrNull()
            val message = "STT request failed ($code): ${errorText ?: "No response body"}"
            if (isPermanentErrorCode(code)) {
                throw SttPermanentException(message)
            }
            throw Exception(message)
        }

        return response.body()?.text ?: throw Exception("Empty transcription response from $sttProvider (Code: ${response.code()})")
    }

    private fun isPermanentErrorCode(code: Int): Boolean {
        return code in setOf(400, 401, 402, 403, 404, 409, 422, 429)
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
            val queuedPaths = repository.getAllAudioPaths().toSet()
            context.cacheDir.listFiles()?.filter {
                it.name.startsWith("recording_") && it.name.endsWith(".m4a") &&
                    it.absolutePath !in queuedPaths
            }?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val KEY_TRANSCRIPTION_ID = "transcription_id"

        fun createInputData(transcriptionId: Long): Data = workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId)
    }

    private class SttPermanentException(message: String) : Exception(message)
}
