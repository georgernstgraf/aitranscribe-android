package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Test class for AudioRecordingButton.
 * Tests recording button UI interactions.
 */
class AudioRecordingButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recordingButtonDisplaysMicrophoneIconWhenNotRecording() {
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
    fun recordingButtonDisplaysStopIconWhenRecording() {
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
    fun recordingButtonShowsDurationWhenRecording() {
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
    fun recordingButtonIsAlwaysClickable() {
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
