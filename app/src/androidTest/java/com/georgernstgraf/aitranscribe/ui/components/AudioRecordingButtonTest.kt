package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.jupiter.api.Test

/**
 * Test class for AudioRecordingButton.
 * Tests recording button UI interactions.
 */
class AudioRecordingButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `recording button displays microphone icon when not recording`() {
        var isRecording = false

        composeTestRule.setContent {
            AudioRecordingButton(
                isRecording = isRecording,
                recordingDuration = 0,
                onStartRecording = { isRecording = true },
                onStopRecording = { isRecording = false }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .assertIsDisplayed()
    }

    @Test
    fun `recording button displays stop icon when recording`() {
        var isRecording = true
        var duration = 15

        composeTestRule.setContent {
            AudioRecordingButton(
                isRecording = isRecording,
                recordingDuration = duration,
                onStartRecording = { isRecording = true },
                onStopRecording = { isRecording = false }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Stop Recording")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("$duration")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking button toggles recording state`() {
        var isRecording = false

        composeTestRule.setContent {
            AudioRecordingButton(
                isRecording = isRecording,
                recordingDuration = 0,
                onStartRecording = { isRecording = true },
                onStopRecording = { isRecording = false }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .performClick()

        composeTestRule.waitForIdle()
        assert("Recording should start after click") { isRecording }

        composeTestRule
            .onNodeWithContentDescription("Stop Recording")
            .performClick()

        composeTestRule.waitForIdle()
        assert("Recording should stop after click") { !isRecording }
    }

    @Test
    fun `recording button shows duration when recording`() {
        val duration = 30

        composeTestRule.setContent {
            AudioRecordingButton(
                isRecording = true,
                recordingDuration = duration,
                onStartRecording = { },
                onStopRecording = { }
            )
        }

        composeTestRule
            .onNodeWithText("$duration")
            .assertIsDisplayed()
    }

    @Test
    fun `recording button is always clickable`() {
        composeTestRule.setContent {
            AudioRecordingButton(
                isRecording = false,
                recordingDuration = 0,
                onStartRecording = { },
                onStopRecording = { }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Start Recording")
            .assertIsEnabled()
    }
}