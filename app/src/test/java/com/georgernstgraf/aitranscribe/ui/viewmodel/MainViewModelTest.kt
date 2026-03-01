package com.georgernstgraf.aitranscribe.ui.viewmodel

import app.cash.turbine.test
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test class for MainViewModel.
 * Tests main screen logic and recording state management.
 */
class MainViewModelTest {

    private lateinit var viewModel: MainViewModel
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        viewModel = MainViewModel(fakeRepository)
    }

    @Test
    fun `initial state has no recording and no transcriptions`() = runTest {
        val initialState = viewModel.uiState.value

        assertFalse("Should not be recording initially", initialState.isRecording)
        assertEquals("Recording duration should be 0 initially", 0, initialState.recordingDuration)
        assertNull("No recording error initially", initialState.recordingError)
        assertTrue("No transcriptions initially", initialState.recentTranscriptions.isEmpty())
    }

    @Test
    fun `startRecording updates state to recording`() = runTest {
        viewModel.startRecording()

        val state = viewModel.uiState.value
        assertTrue("Should be recording after start", state.isRecording)
    }

    @Test
    fun `stopRecording updates state to not recording`() = runTest {
        viewModel.startRecording()
        viewModel.stopRecording()

        val state = viewModel.uiState.value
        assertFalse("Should not be recording after stop", state.isRecording)
    }

    @Test
    fun `updateRecordingDuration updates duration in state`() = runTest {
        val testDuration = 45

        viewModel.updateRecordingDuration(testDuration)

        val state = viewModel.uiState.value
        assertEquals("Recording duration should be updated", testDuration, state.recordingDuration)
    }

    @Test
    fun `setRecordingError updates error in state`() = runTest {
        val errorMessage = "Microphone permission denied"

        viewModel.setRecordingError(errorMessage)

        val state = viewModel.uiState.value
        assertEquals("Recording error should be set", errorMessage, state.recordingError)
    }

    @Test
    fun `clearRecordingError removes error from state`() = runTest {
        viewModel.setRecordingError("Test error")
        viewModel.clearRecordingError()

        val state = viewModel.uiState.value
        assertNull("Recording error should be cleared", state.recordingError)
    }

    @Test
    fun `loadRecentTranscriptions emits unviewed transcriptions`() = runTest {
        val transcription1 = createTestTranscription(id = 1, playedCount = 0)
        val transcription2 = createTestTranscription(id = 2, playedCount = 0)
        val transcription3 = createTestTranscription(id = 3, playedCount = 1)

        fakeRepository.insertAll(
            listOf(
                transcription1.toEntity(),
                transcription2.toEntity(),
                transcription3.toEntity()
            )
        )

        viewModel.uiState.test {
            awaitItem()
            
            skipItems(1)
            
            val state = awaitItem()
            assertEquals(
                "Should load unviewed transcriptions only",
                2,
                state.recentTranscriptions.size
            )
            assertTrue(
                "Should contain only unviewed transcriptions",
                state.recentTranscriptions.all { it.playedCount == 0 }
            )
        }
    }

    @Test
    fun `loadRecentTranscriptions limits to 10 items`() = runTest {
        val transcriptions = (1..15).map { id ->
            createTestTranscription(id = id.toLong(), playedCount = 0)
        }

        fakeRepository.insertAll(transcriptions.map { it.toEntity() })

        viewModel.uiState.test {
            awaitItem()
            
            skipItems(1)
            
            val state = awaitItem()
            assertEquals("Should limit to 10 transcriptions", 10, state.recentTranscriptions.size)
        }
    }

    @Test
    fun `loadRecentTranscriptions orders by created date descending`() = runTest {
        val now = LocalDateTime.now()
        val transcription1 = createTestTranscription(id = 1, createdAt = now.minusDays(3), playedCount = 0)
        val transcription2 = createTestTranscription(id = 2, createdAt = now.minusDays(1), playedCount = 0)
        val transcription3 = createTestTranscription(id = 3, createdAt = now.minusDays(2), playedCount = 0)

        fakeRepository.insertAll(
            listOf(
                transcription1.toEntity(),
                transcription2.toEntity(),
                transcription3.toEntity()
            )
        )

        viewModel.uiState.test {
            awaitItem()
            
            skipItems(1)
            
            val state = awaitItem()
            assertEquals(
                "Should order by created date descending",
                transcription2.id,
                state.recentTranscriptions[0].id
            )
            assertEquals(
                "Should order by created date descending",
                transcription3.id,
                state.recentTranscriptions[1].id
            )
            assertEquals(
                "Should order by created date descending",
                transcription1.id,
                state.recentTranscriptions[2].id
            )
        }
    }

    private fun createTestTranscription(
        id: Long,
        playedCount: Int = 0,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Transcription {
        return Transcription(
            id = id,
            originalText = "Test transcription $id",
            processedText = "Processed $id",
            audioFilePath = "/path/to/audio$id.mp3",
            createdAt = createdAt,
            postProcessingType = null,
            status = TranscriptionStatus.COMPLETED,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
    }

    private fun Transcription.toEntity(): com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity {
        return com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity(
            id = id,
            originalText = originalText,
            processedText = processedText,
            audioFilePath = audioFilePath,
            createdAt = createdAt.toString(),
            postProcessingType = postProcessingType?.name,
            status = status.name,
            errorMessage = errorMessage,
            playedCount = playedCount,
            retryCount = retryCount
        )
    }
}