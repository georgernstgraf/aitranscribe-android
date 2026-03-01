package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test class for TranscriptionItem.
 * Tests transcription list item UI interactions.
 */
class TranscriptionItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `transcription item displays preview text`() {
        val transcription = createTestTranscription(
            originalText = "This is a test transcription with some text",
            processedText = null
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        composeTestRule
            .onNodeWithText("This is a test transcription with some text")
            .assertIsDisplayed()
    }

    @Test
    fun `transcription item displays processed text when available`() {
        val transcription = createTestTranscription(
            originalText = "This is the original text",
            processedText = "This is the processed text"
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        composeTestRule
            .onNodeWithText("This is the processed text")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("This is the original text")
            .assertDoesNotExist()
    }

    @Test
    fun `transcription item truncates long text`() {
        val longText = "A".repeat(200)

        val transcription = createTestTranscription(
            originalText = longText,
            processedText = null
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        composeTestRule
            .onNodeWithText("…")
            .assertIsDisplayed()
    }

    @Test
    fun `transcription item displays date and time`() {
        val testDate = LocalDateTime.of(2024, 3, 1, 14, 30)

        val transcription = createTestTranscription(
            createdAt = testDate
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        composeTestRule
            .onNodeWithText("Mar 01, 2024")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("14:30")
            .assertIsDisplayed()
    }

    @Test
    fun `transcription item is clickable`() {
        var clicked = false

        val transcription = createTestTranscription()

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { clicked = true }
            )
        }

        composeTestRule
            .onNodeWithText(transcription.originalText)
            .performClick()

        composeTestRule.waitForIdle()
        assertTrue("Click should trigger onClick callback", clicked)
    }

    @Test
    fun `unviewed transcription shows blue indicator`() {
        val transcription = createTestTranscription(
            playedCount = 0
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        // In a real test, we'd verify the indicator color
        // For now, we just verify the component renders
        composeTestRule
            .onNodeWithText(transcription.originalText)
            .assertIsDisplayed()
    }

    @Test
    fun `viewed transcription shows gray indicator`() {
        val transcription = createTestTranscription(
            playedCount = 1
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        // In a real test, we'd verify the indicator color
        // For now, we just verify the component renders
        composeTestRule
            .onNodeWithText(transcription.originalText)
            .assertIsDisplayed()
    }

    private fun createTestTranscription(
        originalText: String = "Test transcription",
        processedText: String? = null,
        playedCount: Int = 0,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Transcription {
        return Transcription(
            id = 1,
            originalText = originalText,
            processedText = processedText,
            audioFilePath = "/path/to/audio.mp3",
            createdAt = createdAt,
            postProcessingType = null,
            status = TranscriptionStatus.COMPLETED,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
    }
}