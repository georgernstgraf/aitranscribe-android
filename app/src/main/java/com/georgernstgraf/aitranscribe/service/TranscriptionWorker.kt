package com.georgernstgraf.aitranscribe.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.georgernstgraf.aitranscribe.data.local.AppSettingsStore
import com.georgernstgraf.aitranscribe.data.local.ProviderModelDao
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.ProviderConfig
import com.georgernstgraf.aitranscribe.domain.model.WhisperLanguageMapper
import com.georgernstgraf.aitranscribe.domain.usecase.PostProcessTextUseCase
import com.georgernstgraf.aitranscribe.util.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted private val params: WorkerParameters,
    private val repository: TranscriptionRepository,
    private val groqApiService: GroqApiService,
    private val zaiApiService: ZaiApiService,
    private val networkMonitor: NetworkMonitor,
    private val appSettingsStore: AppSettingsStore,
    private val providerModelDao: ProviderModelDao,
    private val postProcessTextUseCase: PostProcessTextUseCase,
    private val languageRepository: LanguageRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val transcriptionId = inputData.getLong(KEY_TRANSCRIPTION_ID, -1)
        if (transcriptionId == -1L) return@withContext Result.failure()

        val transcription = repository.getById(transcriptionId) ?: return@withContext Result.failure()
        val audioPath = transcription.audioFilePath ?: return@withContext Result.failure()

        if (!networkMonitor.isConnected()) {
            repository.recordError(transcriptionId, "No network available")
            return@withContext Result.failure()
        }

        val sttProvider = appSettingsStore.getSttProvider()
        val sttModel = appSettingsStore.getProviderSttModel(sttProvider, ProviderConfig.getDefaultSttModel(sttProvider))
        val llmProvider = appSettingsStore.getLlmProvider()
        val llmModel = appSettingsStore.getProviderLlmModel(llmProvider, ProviderConfig.getDefaultLlmModel(llmProvider))

        val transcriptionResult = try {
            transcribeAudio(audioPath, sttModel)
        } catch (e: AudioFileMissingException) {
            Log.w("TranscriptionWorker", "Audio file missing for transcriptionId=$transcriptionId", e)
            repository.markAudioMissing(
                id = transcriptionId,
                errorMessage = "Audio file missing before transcription. It may have been removed by system cleanup."
            )
            cleanupOrphanedAudioFiles()
            return@withContext Result.success(workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId))
        } catch (e: SttPermanentException) {
            Log.e("TranscriptionWorker", "Permanent STT failure", e)
            repository.recordError(transcriptionId, e.message ?: "Permanent STT failure")
            return@withContext Result.failure()
        } catch (e: Exception) {
            Log.e("TranscriptionWorker", "Retryable STT failure", e)
            repository.recordError(transcriptionId, e.message ?: "STT failure")
            return@withContext Result.failure()
        }

        val resolvedLanguageId = transcriptionResult.language?.let { whisperName ->
            val code = WhisperLanguageMapper.mapToCode(whisperName)
            languageRepository.ensureLanguageExists(code)
            code
        }

        repository.markSttSuccess(
            id = transcriptionId,
            sttText = transcriptionResult.text,
            languageId = resolvedLanguageId
        )
        cleanupAudioFile(audioPath)

        val llmApiKey = appSettingsStore.getActiveAuthToken(llmProvider)

        val postProcessingType = PostProcessingType.RAW

        if (!llmApiKey.isNullOrBlank()) {
            try {
                performPostProcessing(
                    transcriptionId = transcriptionId,
                    postProcessingType = postProcessingType,
                    llmModel = llmModel,
                    llmApiKey = llmApiKey,
                    llmProvider = llmProvider
                )
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
                            appSettingsStore.setProviderLlmModel("zai", fallbackModel)
                            cleanupOrphanedAudioFiles()
                            return@withContext Result.success(workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId))
                        } catch (retryError: Exception) {
                            Log.e("TranscriptionWorker", "ZAI fallback post-processing failed (non-fatal)", retryError)
                        }
                    }
                }
                Log.e("TranscriptionWorker", "Post-processing failed (non-fatal)", e)
                repository.recordError(transcriptionId, "Post-processing skipped: ${e.message}")
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
            PostProcessingType.RAW -> {
                postProcessTextUseCase.generateSummary(
                    transcriptionId = transcriptionId,
                    llmModel = llmModel,
                    apiKey = llmApiKey,
                    llmProvider = llmProvider,
                    debugContext = "worker:summary:raw"
                )
            }
            PostProcessingType.CLEANUP -> {
                postProcessTextUseCase(
                    transcriptionId = transcriptionId,
                    postProcessingType = PostProcessingType.CLEANUP,
                    llmModel = llmModel,
                    apiKey = llmApiKey,
                    llmProvider = llmProvider,
                    debugContext = "worker:cleanup"
                )
            }
            else -> {
                // Translation post-processing not used in worker
            }
        }
    }

    private suspend fun resolveZaiFallbackModel(currentModel: String): String? {
        val dynamic = providerModelDao.getModelsForProvider("zai").map { it.externalId }
        val ordered = dynamic.sortedWith(
            compareByDescending<String> { it.contains("4.5-flash", ignoreCase = true) }
                .thenByDescending { it.contains("flash", ignoreCase = true) }
        )
        return ordered.firstOrNull { it != currentModel }
    }

    private suspend fun transcribeAudio(audioPath: String, sttModel: String): TranscriptionResult {
        val audioFile = File(audioPath)
        if (!audioFile.exists()) throw AudioFileMissingException("Audio file not found: $audioPath")

        val sttProvider = appSettingsStore.getSttProvider()
        val token = appSettingsStore.getActiveAuthToken(sttProvider)
        if (token.isNullOrBlank()) throw Exception("${sttProvider.replaceFirstChar { it.uppercase() }} API token not configured")
        val authorization = if (token.startsWith("Bearer ")) token else "Bearer $token"

        val response = when (sttProvider) {
            "groq" -> groqApiService.transcribeAudio(
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

        val body = response.body() ?: throw Exception("Empty transcription response from $sttProvider (Code: ${response.code()})")
        Log.d("TranscriptionWorker", "STT response from $sttProvider: language=${body.language ?: "null"}, text length=${body.text.length}")
        return TranscriptionResult(text = body.text, language = body.language)
    }

    data class TranscriptionResult(
        val text: String,
        val language: String?
    )

    private fun isPermanentErrorCode(code: Int): Boolean {
        return code in setOf(400, 401, 402, 403, 404, 409, 422, 429)
    }

    private fun createAudioPart(audioFile: File): MultipartBody.Part {
        val mediaType = "audio/mp4".toMediaType()
        val requestBody = audioFile.asRequestBody(mediaType)
        return MultipartBody.Part.createFormData("file", audioFile.name, requestBody)
    }

    private fun createModelPart(model: String) = model.toRequestBody("text/plain".toMediaType())

    private fun createFormatPart() = "verbose_json".toRequestBody("text/plain".toMediaType())

    private fun cleanupAudioFile(path: String) {
        try {
            File(path).delete()
        } catch (_: Exception) {
        }
    }

    private suspend fun cleanupOrphanedAudioFiles() {
        try {
            val queuedPaths = repository.getAllAudioPaths().toSet()
            val thresholdTime = System.currentTimeMillis() - ORPHAN_FILE_GRACE_PERIOD_MS
            recordingsDirectory().listFiles()?.filter {
                it.name.startsWith("recording_") && it.name.endsWith(".m4a") &&
                    it.lastModified() < thresholdTime &&
                    it.absolutePath !in queuedPaths
            }?.forEach { it.delete() }
        } catch (_: Exception) {
        }
    }

    private fun recordingsDirectory(): File {
        val directory = File(context.filesDir, RECORDINGS_DIR_NAME)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    companion object {
        const val KEY_TRANSCRIPTION_ID = "transcription_id"
        private const val RECORDINGS_DIR_NAME = "recordings"
        private const val ORPHAN_FILE_GRACE_PERIOD_MS = 15 * 60 * 1000L

        fun createInputData(transcriptionId: Long): Data = workDataOf(KEY_TRANSCRIPTION_ID to transcriptionId)
    }

    private class SttPermanentException(message: String) : Exception(message)
    private class AudioFileMissingException(message: String) : IOException(message)
}
