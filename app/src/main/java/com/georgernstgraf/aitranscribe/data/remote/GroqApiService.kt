package com.georgernstgraf.aitranscribe.data.remote

import com.georgernstgraf.aitranscribe.data.remote.dto.GroqTranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.GET
import retrofit2.http.Query
import com.georgernstgraf.aitranscribe.data.remote.dto.ModelsResponse

/**
 * GROQ API Service for speech-to-text transcription.
 */
interface GroqApiService {

    @GET("openai/v1/models")
    suspend fun getModels(
        @Header("Authorization") authorization: String
    ): Response<ModelsResponse>

    @POST("openai/v1/audio/transcriptions")
    @Multipart
    suspend fun transcribeAudio(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody
    ): Response<GroqTranscriptionResponse>

    companion object {
        const val BASE_URL = "https://api.groq.com/"
    }
}