package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case for transcribing audio using GROQ API.
 */
@ViewModelScoped
class TranscribeAudioUseCase @Inject constructor(
    private val groqApiService: GroqApiService,
    private val repository: TranscriptionRepository
) {

    suspend operator fun invoke(
        audioPath: String,
        sttModel: String,
        apiKey: String
    ): TranscriptionResult = withContext(Dispatchers.IO) {
        if (audioPath.isBlank()) {
            throw TranscriptionException("Audio path cannot be empty")
        }

        if (apiKey.isBlank()) {
            throw TranscriptionException("API key cannot be empty")
        }

        try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                throw TranscriptionException("Audio file does not exist: $audioPath")
            }

            val requestFile = audioFile.asRequestBody("audio/*".toMediaTypeOrNull())

            val filePart = MultipartBody.Part.createFormData(
                "file",
                audioFile.name,
                requestFile
            )

            val modelPart = sttModel.toRequestBody("text/plain".toMediaTypeOrNull())

            val responseFormatPart = "text".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = groqApiService.transcribeAudio(
                authorization = "Bearer $apiKey",
                file = filePart,
                model = modelPart,
                responseFormat = responseFormatPart
            )

            if (!response.isSuccessful || response.body() == null) {
                throw TranscriptionException(
                    message = "Transcription failed: ${response.message()}",
                    errorCode = response.code()
                )
            }

            val transcriptionText = response.body()!!.text

            val entity = TranscriptionEntity(
                text = transcriptionText,
                audioFilePath = audioPath,
                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                status = TranscriptionStatus.COMPLETED.name,
                errorMessage = null,
                seen = false
            )

            val id = repository.insert(entity)

            TranscriptionResult(
                id = id,
                isSuccess = true,
                text = transcriptionText
            )
        } catch (e: TranscriptionException) {
            throw e
        } catch (e: Exception) {
            throw TranscriptionException("Failed to transcribe audio: ${e.message}", e)
        }
    }

    data class TranscriptionResult(
        val id: Long,
        val isSuccess: Boolean,
        val text: String? = null
    )

    class TranscriptionException(
        message: String,
        cause: Throwable? = null,
        val errorCode: Int? = null
    ) : Exception(message, cause)
}
