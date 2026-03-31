package com.georgernstgraf.aitranscribe.data.remote

import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterRequest
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ZaiCodingApiService {

    @POST("chat/completions")
    suspend fun processText(
        @Header("Authorization") authorization: String,
        @Body request: OpenRouterRequest
    ): Response<OpenRouterResponse>

    companion object {
        const val BASE_URL = "https://api.z.ai/api/coding/paas/v4/"
    }
}
