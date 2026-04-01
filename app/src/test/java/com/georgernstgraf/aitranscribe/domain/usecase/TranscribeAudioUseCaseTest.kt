package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.remote.GroqApiService
import com.georgernstgraf.aitranscribe.data.remote.dto.GroqTranscriptionResponse
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Response
import java.io.File

class TranscribeAudioUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var apiService: GroqApiService
    private lateinit var useCase: TranscribeAudioUseCase
    private lateinit var tempAudioFile: File

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        apiService = mockk()
        useCase = TranscribeAudioUseCase(apiService, repository)
        
        // Create a temporary audio file for testing
        tempAudioFile = File(System.getProperty("java.io.tmpdir"), "test_audio.mp3")
        tempAudioFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5))
    }

    @Test
    fun `invoke transcribes audio and saves to repository`() = runTest {
        coEvery {
            apiService.transcribeAudio(any(), any(), any(), any())
        } returns Response.success(
            GroqTranscriptionResponse(text = "Hello world")
        )

        val result = useCase(tempAudioFile.absolutePath, "whisper-large-v3", "test-key")

        assertTrue(result.isSuccess)
        assertEquals("Hello world", result.text)
        assertEquals(1, repository.getCount())
    }

    @Test
    fun `invoke throws exception when audio path is empty`() {
        assertThrows(TranscribeAudioUseCase.TranscriptionException::class.java) {
            runTest { useCase("", "whisper-large-v3", "test-key") }
        }
    }

    @Test
    fun `invoke throws exception when API key is empty`() {
        assertThrows(TranscribeAudioUseCase.TranscriptionException::class.java) {
            runTest { useCase(tempAudioFile.absolutePath, "whisper-large-v3", "") }
        }
    }

    @Test
    fun `invoke throws exception when file does not exist`() {
        assertThrows(TranscribeAudioUseCase.TranscriptionException::class.java) {
            runTest { useCase("/non/existent/file.mp3", "whisper-large-v3", "test-key") }
        }
    }
}
