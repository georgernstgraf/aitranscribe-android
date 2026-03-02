package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test class for TranscribeAudioUseCase.
 * Tests audio transcription business logic.
 */
class TranscribeAudioUseCaseTest {

    private lateinit var useCase: TranscribeAudioUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository
    private lateinit var fakeGroqService: FakeGroqApiService

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        fakeGroqService = FakeGroqApiService()
        useCase = TranscribeAudioUseCase(fakeGroqService, fakeRepository)
    }

    @Test
    fun `transcribe audio stores transcription in repository`() = runTest {
        val audioPath = "/path/to/audio.mp3"

        val result = useCase.invoke(
            audioPath = audioPath,
            sttModel = "whisper-large-v3-turbo"
        )

        assertTrue("Transcription should succeed", result.isSuccess)
        
        val transcription = fakeRepository.getById(result.id)
        assertNotNull("Transcription should be stored in repository", transcription)
    }

    @Test
    fun `transcribe audio returns transcription id`() = runTest {
        val audioPath = "/path/to/audio.mp3"

        val result = useCase.invoke(
            audioPath = audioPath,
            sttModel = "whisper-large-v3-turbo"
        )

        assertTrue("Should return positive transcription id", result.id > 0)
    }

    @Test
    fun `transcribe audio stores original text`() = runTest {
        val audioPath = "/path/to/audio.mp3"
        val expectedText = "This is the transcribed text"

        fakeGroqService.setTranscriptionText(expectedText)

        val result = useCase.invoke(
            audioPath = audioPath,
            sttModel = "whisper-large-v3-turbo"
        )

        val transcription = fakeRepository.getById(result.id)
        assertEquals("Original text should be stored", expectedText, transcription?.originalText)
    }

    @Test
    fun `transcribe audio sets status to completed`() = runTest {
        val audioPath = "/path/to/audio.mp3"

        val result = useCase.invoke(
            audioPath = audioPath,
            sttModel = "whisper-large-v3-turbo"
        )

        val transcription = fakeRepository.getById(result.id)
        assertEquals("Status should be completed", TranscriptionStatus.COMPLETED, transcription?.status)
    }

    @Test(expected = TranscriptionException::class)
    fun `transcribe audio throws exception for empty audio path`() = runTest {
        useCase.invoke(
            audioPath = "",
            sttModel = "whisper-large-v3-turbo"
        )
    }

    @Test(expected = TranscriptionException::class)
    fun `transcribe audio throws exception on API failure`() = runTest {
        fakeGroqService.setShouldFail(true)

        useCase.invoke(
            audioPath = "/path/to/audio.mp3",
            sttModel = "whisper-large-v3-turbo"
        )
    }

    @Test
    fun `transcribe audio stores audio file path`() = runTest {
        val audioPath = "/path/to/audio.mp3"

        val result = useCase.invoke(
            audioPath = audioPath,
            sttModel = "whisper-large-v3-turbo"
        )

        val transcription = fakeRepository.getById(result.id)
        assertEquals("Audio file path should be stored", audioPath, transcription?.audioFilePath)
    }

    data class TranscriptionResult(
        val id: Long,
        val isSuccess: Boolean
    )

    class FakeGroqApiService {
        private var transcriptionText = "Default transcription"
        private var shouldFail = false

        fun setTranscriptionText(text: String) {
            transcriptionText = text
        }

        fun setShouldFail(fail: Boolean) {
            shouldFail = fail
        }

        suspend fun transcribe(audioPath: String, model: String): String {
            if (shouldFail) {
                throw Exception("API Error")
            }
            return transcriptionText
        }
    }

    class TranscriptionException(message: String) : Exception(message)
}