package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.domain.util.PromptManager
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.OpenRouterApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterResponse
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterChoice
import com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterMessage
import com.georgernstgraf.aitranscribe.data.remote.ZaiApiService
import com.georgernstgraf.aitranscribe.data.remote.ZaiCodingApiService
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.TranslationTarget
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private lateinit var zaiApiService: ZaiApiService
    private lateinit var zaiCodingApiService: ZaiCodingApiService
    private lateinit var groqApiService: GroqApiService
    private lateinit var promptManager: PromptManager
    private lateinit var useCase: PostProcessTextUseCase

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
        apiService = mockk()
        zaiApiService = mockk()
        zaiCodingApiService = mockk()
        groqApiService = mockk()
        promptManager = mockk()
        useCase = PostProcessTextUseCase(
            apiService,
            zaiApiService,
            zaiCodingApiService,
            groqApiService,
            repository,
            promptManager
        )

        every { promptManager.get(any()) } answers {
            when (val key = firstArg<String>()) {
                "prompt.system.base" -> "BASE"
                "prompt.system.request" -> "User request: {{request}}"
                "prompt.user.transcription" -> "Here is the transcription:\n\n{{text}}"
                else -> key
            }
        }
    }

    @Test
    fun `invoke processes text with LLM`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                text = "Hello world",
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                status = "COMPLETED",
                errorMessage = null,
                seen = false
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
        assertEquals("Processed text", updated?.text)
    }

    @Test
    fun `detail action skips llm when language matches button and cleanup disabled`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                text = "Already English text",
                audioFilePath = null,
                createdAt = LocalDateTime.now().toString(),
                status = "COMPLETED",
                errorMessage = null,
                seen = false,
                language = "en"
            )
        )

        useCase(
            transcriptionId = id,
            isCleanupEnabled = false,
            translationTarget = TranslationTarget.EN,
            llmModel = "test-model",
            apiKey = "test-key"
        )

        coVerify(exactly = 0) { apiService.processText(any(), any()) }
        val unchanged = repository.getById(id)
        assertEquals("Already English text", unchanged?.text)
        assertEquals("en", unchanged?.language)
    }

    @Test
    fun `detail action translates plus cleanup then updates language and summary`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                text = "Original unknown language",
                audioFilePath = null,
                createdAt = LocalDateTime.now().toString(),
                status = "COMPLETED",
                errorMessage = null,
                seen = false,
                language = null
            )
        )

        val requests = mutableListOf<com.georgernstgraf.aitranscribe.data.remote.dto.OpenRouterRequest>()
        coEvery { apiService.processText(any(), capture(requests)) } returnsMany listOf(
            Response.success(
                OpenRouterResponse(
                    choices = listOf(OpenRouterChoice(OpenRouterMessage("assistant", "Translated and cleaned")))
                )
            ),
            Response.success(
                OpenRouterResponse(
                    choices = listOf(OpenRouterChoice(OpenRouterMessage("assistant", "Summary text")))
                )
            )
        )

        useCase(
            transcriptionId = id,
            isCleanupEnabled = true,
            translationTarget = TranslationTarget.DE,
            llmModel = "test-model",
            apiKey = "test-key"
        )

        coVerify(exactly = 2) { apiService.processText(any(), any()) }
        val postProcessPrompt = requests.first().messages.first().content
        assertTrue(postProcessPrompt.contains("prompt.cleanup"))
        assertTrue(postProcessPrompt.contains("prompt.cleanup.de"))

        val updated = repository.getById(id)
        assertEquals("Translated and cleaned", updated?.text)
        assertEquals("de", updated?.language)
        assertEquals("Summary text", updated?.summary)
    }

    @Test(expected = PostProcessTextUseCase.PostProcessingException::class)
    fun `invoke throws exception when API key is empty`() = runTest {
        useCase(1L, PostProcessingType.CLEANUP, "test-model", "")
    }
}
