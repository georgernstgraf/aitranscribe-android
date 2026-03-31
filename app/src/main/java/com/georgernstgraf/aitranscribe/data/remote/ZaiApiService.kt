package com.georgernstgraf.aitranscribe.data.remote

import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterRequest
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.GET
import com.georgernstgraf.aitranscribe.data.remote.dto.ModelsResponse

import retrofit2.http.Part
import retrofit2.http.Multipart
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.georgernstgraf.aitranscribe.data.remote.dto.GroqTranscriptionResponse

interface ZaiApiService {

    @Multipart
    @POST("paas/v4/audio/transcriptions")
    suspend fun transcribeAudio(
        @Header("Authorization") authorization: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("response_format") responseFormat: RequestBody
    ): Response<GroqTranscriptionResponse> // Standard OpenAI compatible STT format

    @GET("paas/v4/models")
    suspend fun getModels(
        @Header("Authorization") authorization: String
    ): Response<ModelsResponse>

    @POST("paas/v4/chat/completions")
    suspend fun processText(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>

    companion object {
        const val BASE_URL = "https://api.z.ai/api/"
    }
}
