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
import com.georgernstgraf.aitranscribe.domain.repository.LanguageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.Response
import java.time.LocalDateTime

class PostProcessTextUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var apiService: OpenRouterApiService
    private lateinit var zaiApiService: ZaiApiService
    private lateinit var zaiCodingApiService: ZaiCodingApiService
    private lateinit var groqApiService: GroqApiService
    private lateinit var promptManager: PromptManager
    private lateinit var languageRepository: LanguageRepository
    private lateinit var useCase: PostProcessTextUseCase

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        apiService = mockk()
        zaiApiService = mockk()
        zaiCodingApiService = mockk()
        groqApiService = mockk()
        promptManager = mockk()
        languageRepository = mockk()
        useCase = PostProcessTextUseCase(
            apiService,
            zaiApiService,
            zaiCodingApiService,
            groqApiService,
            repository,
            promptManager,
            languageRepository
        )

        every { promptManager.get(any()) } answers {
            when (val key = firstArg<String>()) {
                "prompt.system.base" -> "BASE"
                "prompt.system.request" -> "User request: {{request}}"
                "prompt.user.transcription" -> "Here is the transcription:\n\n{{text}}"
                "prompt.cleanup" -> "Cleanup in {{language}}"
                "prompt.translate" -> "Translate to {{language}}"
                "prompt.summary" -> "Summary in {{language}}"
                else -> key
            }
        }

        coEvery { languageRepository.getLanguageName(any()) } answers {
            when (val code = firstArg<String>()) {
                "en" -> "English"
                "de" -> "German"
                else -> code
            }
        }
    }

    @Test
    fun `invoke processes text with LLM`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Hello world",
                cleanedText = null,
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
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
        assertEquals("Processed text", updated?.cleanedText)
    }

    @Test
    fun `detail action skips llm when language matches target and cleanup disabled`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Already English text",
                cleanedText = null,
                audioFilePath = null,
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false,
                languageId = "en"
            )
        )

        useCase(
            transcriptionId = id,
            isCleanupEnabled = false,
            targetLanguage = "en",
            llmModel = "test-model",
            apiKey = "test-key"
        )

        coVerify(exactly = 0) { apiService.processText(any(), any()) }
        val unchanged = repository.getById(id)
        assertEquals("Already English text", unchanged?.sttText)
        assertEquals("en", unchanged?.languageId)
    }

    @Test
    fun `detail action translates plus cleanup then updates language and summary`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Original unknown language",
                cleanedText = null,
                audioFilePath = null,
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false,
                languageId = null
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
            targetLanguage = "de",
            llmModel = "test-model",
            apiKey = "test-key"
        )

        coVerify(exactly = 2) { apiService.processText(any(), any()) }
        val postProcessPrompt = requests.first().messages.first().content
        assertTrue(postProcessPrompt.contains("Translate"))
        assertTrue(postProcessPrompt.contains("German"))

        val updated = repository.getById(id)
        assertEquals("Translated and cleaned", updated?.cleanedText)
        assertEquals("de", updated?.languageId)
        assertEquals("Summary text", updated?.summary)
    }

    @Test
    fun `invoke throws exception when API key is empty`() = runTest {
        val id = repository.insert(
            TranscriptionEntity(
                id = 0,
                sttText = "Test text",
                cleanedText = null,
                audioFilePath = null,
                createdAt = LocalDateTime.now().toString(),
                errorMessage = null,
                seen = false
            )
        )

        assertThrows<PostProcessTextUseCase.PostProcessingException> {
            useCase(id, PostProcessingType.CLEANUP, "test-model", "")
        }
    }
}
