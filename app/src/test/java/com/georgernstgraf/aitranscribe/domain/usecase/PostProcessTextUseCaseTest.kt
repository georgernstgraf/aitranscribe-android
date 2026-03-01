package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.PostProcessingType
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test class for PostProcessTextUseCase.
 * Tests LLM post-processing business logic.
 */
class PostProcessTextUseCaseTest {

    private lateinit var useCase: PostProcessTextUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository
    private lateinit var fakeOpenRouterService: FakeOpenRouterApiService

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        fakeOpenRouterService = FakeOpenRouterApiService()
        useCase = PostProcessTextUseCase(fakeOpenRouterService, fakeRepository)
    }

    @Test
    fun `post processing updates transcription with processed text`() = runTest {
        val transcriptionId = createTestTranscription()
        val processedText = "This is processed text"

        fakeOpenRouterService.setProcessedText(processedText)

        useCase.invoke(
            transcriptionId = transcriptionId,
            postProcessingType = PostProcessingType.GRAMMAR,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = "test-key"
        )

        val transcription = fakeRepository.getById(transcriptionId)
        assertEquals("Processed text should be updated", processedText, transcription?.processedText)
    }

    @Test
    fun `post processing stores post processing type`() = runTest {
        val transcriptionId = createTestTranscription()

        useCase.invoke(
            transcriptionId = transcriptionId,
            postProcessingType = PostProcessingType.ENGLISH,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = "test-key"
        )

        val transcription = fakeRepository.getById(transcriptionId)
        assertEquals(
            "Post processing type should be stored",
            PostProcessingType.ENGLISH.name,
            transcription?.postProcessingType
        )
    }

    @Test
    fun `post processing uses correct prompt for grammar`() = runTest {
        val transcriptionId = createTestTranscription()
        val originalText = "This is original text"

        fakeOpenRouterService.setProcessedText("Processed")

        useCase.invoke(
            transcriptionId = transcriptionId,
            postProcessingType = PostProcessingType.GRAMMAR,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = "test-key"
        )

        val lastRequest = fakeOpenRouterService.getLastRequest()
        assertTrue(
            "Grammar prompt should include correction instructions",
            lastRequest.messages.any { it.content.contains("correct grammatical errors") }
        )
    }

    @Test
    fun `post processing uses correct prompt for english translation`() = runTest {
        val transcriptionId = createTestTranscription()

        fakeOpenRouterService.setProcessedText("Processed")

        useCase.invoke(
            transcriptionId = transcriptionId,
            postProcessingType = PostProcessingType.ENGLISH,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = "test-key"
        )

        val lastRequest = fakeOpenRouterService.getLastRequest()
        assertTrue(
            "English prompt should include translation instructions",
            lastRequest.messages.any { it.content.contains("translate") }
        )
    }

    @Test(expected = PostProcessingException::class)
    fun `post processing throws exception for empty API key`() = runTest {
        val transcriptionId = createTestTranscription()

        useCase.invoke(
            transcriptionId = transcriptionId,
            postProcessingType = PostProcessingType.GRAMMAR,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = ""
        )
    }

    @Test(expected = PostProcessingException::class)
    fun `post processing throws exception for non-existent transcription`() = runTest {
        useCase.invoke(
            transcriptionId = 999,
            postProcessingType = PostProcessingType.GRAMMAR,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = "test-key"
        )
    }

    @Test(expected = PostProcessingException::class)
    fun `post processing throws exception on API failure`() = runTest {
        val transcriptionId = createTestTranscription()
        fakeOpenRouterService.setShouldFail(true)

        useCase.invoke(
            transcriptionId = transcriptionId,
            postProcessingType = PostProcessingType.GRAMMAR,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = "test-key"
        )
    }

    @Test
    fun `post processing does not change original text`() = runTest {
        val originalText = "This is original text"
        val transcriptionId = createTestTranscription(originalText = originalText)

        fakeOpenRouterService.setProcessedText("Processed text")

        useCase.invoke(
            transcriptionId = transcriptionId,
            postProcessingType = PostProcessingType.GRAMMAR,
            llmModel = "anthropic/claude-3-haiku",
            apiKey = "test-key"
        )

        val transcription = fakeRepository.getById(transcriptionId)
        assertEquals("Original text should not change", originalText, transcription?.originalText)
    }

    private fun createTestTranscription(originalText: String = "Test transcription"): Long {
        val entity = TranscriptionEntity(
            id = 0,
            originalText = originalText,
            processedText = null,
            audioFilePath = "/path/to/audio.mp3",
            createdAt = LocalDateTime.now().toString(),
            postProcessingType = null,
            status = TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = 0,
            retryCount = 0
        )
        return fakeRepository.insert(entity)
    }

    class FakeOpenRouterApiService {
        private var processedText = "Default processed"
        private var shouldFail = false
        private val requests = mutableListOf<OpenRouterRequest>()

        fun setProcessedText(text: String) {
            processedText = text
        }

        fun setShouldFail(fail: Boolean) {
            shouldFail = fail
        }

        fun getLastRequest(): OpenRouterRequest = requests.last()

        suspend fun processText(request: OpenRouterRequest): OpenRouterResponse {
            requests.add(request)
            
            if (shouldFail) {
                throw Exception("API Error")
            }

            return OpenRouterResponse(
                choices = listOf(
                    OpenRouterChoice(
                        message = OpenRouterMessage(
                            role = "assistant",
                            content = processedText
                        )
                    )
                )
            )
        }
    }

    data class OpenRouterRequest(
        val model: String,
        val messages: List<OpenRouterMessage>
    )

    data class OpenRouterResponse(
        val choices: List<OpenRouterChoice>
    )

    data class OpenRouterChoice(
        val message: OpenRouterMessage
    )

    data class OpenRouterMessage(
        val role: String,
        val content: String
    )

    class PostProcessingException(message: String) : Exception(message)
}