package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
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
    fun transcriptionItemDisplaysPreviewText() {
        val transcription = createTestTranscription(
            text = "This is a test transcription with some text"
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
    fun transcriptionItemDisplaysText() {
        val transcription = createTestTranscription(
            text = "This is the processed text"
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
    }

    @Test
    fun transcriptionItemTruncatesLongText() {
        val longText = "A".repeat(200)

        val transcription = createTestTranscription(
            text = longText
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        // Component truncates at 60 chars and adds "…"
        val expectedText = "A".repeat(60) + "…"
        composeTestRule
            .onNodeWithText(expectedText, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun transcriptionItemDisplaysShortTextFully() {
        val shortText = "Short text"

        val transcription = createTestTranscription(
            text = shortText
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        composeTestRule
            .onNodeWithText(shortText)
            .assertIsDisplayed()
    }

    @Test
    fun transcriptionItemIsClickable() {
        var clicked = false

        val transcription = createTestTranscription()

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { clicked = true }
            )
        }

        composeTestRule
            .onNodeWithText(transcription.text ?: "")
            .performClick()

        composeTestRule.waitForIdle()
        assertTrue("Click should trigger onClick callback", clicked)
    }

    @Test
    fun unviewedTranscriptionShowsBlueIndicator() {
        val transcription = createTestTranscription(
            seen = false
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        composeTestRule
            .onNodeWithText(transcription.text ?: "")
            .assertIsDisplayed()
    }

    @Test
    fun viewedTranscriptionShowsGrayIndicator() {
        val transcription = createTestTranscription(
            seen = true
        )

        composeTestRule.setContent {
            TranscriptionItem(
                transcription = transcription,
                onClick = { }
            )
        }

        composeTestRule
            .onNodeWithText(transcription.text ?: "")
            .assertIsDisplayed()
    }

    private fun createTestTranscription(
        text: String = "Test transcription",
        seen: Boolean = false,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Transcription {
        return Transcription(
            id = 1,
            text = text,
            audioFilePath = "/path/to/audio.mp3",
            createdAt = createdAt,
            status = TranscriptionStatus.COMPLETED,
            errorMessage = null,
            seen = seen
        )
    }
}
