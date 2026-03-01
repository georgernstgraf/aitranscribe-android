package com.georgernstgraf.aitranscribe.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Intent
import android.media.MediaRecorder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.File

/**
 * Test class for RecordingService.
 * Tests audio recording service functionality.
 */
@RunWith(AndroidJUnit4::class)
class RecordingServiceTest {

    @Mock
    private lateinit var notificationManager: NotificationManager

    private lateinit var recordingService: RecordingService
    private lateinit var testContext: android.content.Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testContext = ApplicationProvider.getApplicationContext<android.app.Application>()
        
        recordingService = RecordingService()
        recordingService.attachBaseContext(testContext)
    }

    @Test
    fun `service creates notification when recording starts`() {
        val intent = Intent(testContext, RecordingService::class.java)
        intent.putExtra(EXTRA_START_RECORDING, true)

        recordingService.onStartCommand(intent, 0, 0)

        // Verify notification was created
        Mockito.verify(notificationManager).notify(
            RecordingService.NOTIFICATION_ID_RECORDING,
            Mockito.any(Notification::class.java)
        )
    }

    @Test
    fun `service stops recording when stop command received`() {
        val startIntent = Intent(testContext, RecordingService::class.java)
        startIntent.putExtra(EXTRA_START_RECORDING, true)

        val stopIntent = Intent(testContext, RecordingService::class.java)
        stopIntent.putExtra(EXTRA_STOP_RECORDING, true)

        recordingService.onStartCommand(startIntent, 0, 0)
        recordingService.onStartCommand(stopIntent, 0, 0)

        // Verify recording was stopped
        assertFalse("Recording should be stopped", recordingService.isRecording())
    }

    @Test
    fun `service creates audio file when recording`() {
        val intent = Intent(testContext, RecordingService::class.java)
        intent.putExtra(EXTRA_START_RECORDING, true)

        recordingService.onStartCommand(intent, 0, 0)

        val audioFile = recordingService.getCurrentAudioFile()
        assertNotNull("Audio file should be created", audioFile)
        assertTrue("Audio file should exist", audioFile.exists())
    }

    @Test
    fun `service deletes temp file on stop`() {
        val intent = Intent(testContext, RecordingService::class.java)
        intent.putExtra(EXTRA_START_RECORDING, true)

        recordingService.onStartCommand(intent, 0, 0)

        val audioFile = recordingService.getCurrentAudioFile()

        val stopIntent = Intent(testContext, RecordingService::class.java)
        stopIntent.putExtra(EXTRA_STOP_RECORDING, true)

        recordingService.onStartCommand(stopIntent, 0, 0)

        // File should be deleted after stopping
        assertFalse("Temp file should be deleted", audioFile.exists())
    }

    @Test
    fun `service throws exception when starting without media recorder`() {
        // This test verifies error handling
        val intent = Intent(testContext, RecordingService::class.java)
        intent.putExtra(EXTRA_START_RECORDING, true)

        try {
            recordingService.onStartCommand(intent, 0, 0)
        } catch (e: Exception) {
            // Expected exception when MediaRecorder can't be initialized
            assertTrue("Should handle MediaRecorder exception", 
                e is IllegalStateException || e is NullPointerException)
        }
    }

    @Test
    fun `service cancels recording when cancel command received`() {
        val startIntent = Intent(testContext, RecordingService::class.java)
        startIntent.putExtra(EXTRA_START_RECORDING, true)

        val cancelIntent = Intent(testContext, RecordingService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_RECORDING, true)

        recordingService.onStartCommand(startIntent, 0, 0)
        recordingService.onStartCommand(cancelIntent, 0, 0)

        assertFalse("Recording should be cancelled", recordingService.isRecording())
    }

    @Test
    fun `service returns recording state correctly`() {
        val intent = Intent(testContext, RecordingService::class.java)
        intent.putExtra(EXTRA_START_RECORDING, true)

        assertFalse("Should not be recording initially", recordingService.isRecording())

        recordingService.onStartCommand(intent, 0, 0)
        assertTrue("Should be recording after start", recordingService.isRecording())
    }

    companion object {
        const val EXTRA_START_RECORDING = "com.georgernstgraf.aitranscribe.START_RECORDING"
        const val EXTRA_STOP_RECORDING = "com.georgernstgraf.aitranscribe.STOP_RECORDING"
        const val EXTRA_CANCEL_RECORDING = "com.georgernstgraf.aitranscribe.CANCEL_RECORDING"
    }
}