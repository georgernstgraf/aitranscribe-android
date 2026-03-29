package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterResponse
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterChoice
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterMessage
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.time.LocalDateTime

class PostProcessTextUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var apiService: OpenRouterApiService
    private lateinit var useCase: PostProcessTextUseCase

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
        apiService = mockk()
        useCase = PostProcessTextUseCase(apiService, repository)
    }

    @Test
    fun `invoke processes text with LLM`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                originalText = "Hello world",
                processedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                postProcessingType = null,
                status = "COMPLETED",
                errorMessage = null,
                playedCount = 0,
                retryCount = 0
            )
        )

        coEvery {
            apiService.processText(any(), any())
        } returns Response.success(
            OpenRouterResponse(
                choices = listOf(
                    OpenRouterChoice(
                        message = OpenRouterMessage(
                            role = "assistant",
                            content = "Processed text"
                        )
                    )
                )
            )
        )

        useCase(id, PostProcessingType.CLEANUP, "test-model", "test-key")

        val updated = repository.getById(id)
        assertEquals("Processed text", updated?.originalText)
    }

    @Test(expected = PostProcessTextUseCase.PostProcessingException::class)
    fun `invoke throws exception when API key is empty`() = runTest {
        useCase(1L, PostProcessingType.CLEANUP, "test-model", "")
    }
}
