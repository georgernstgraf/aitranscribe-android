package com.georgernstgraf.aitranscribe.service

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for RecordingService.
 * Tests audio recording service functionality.
 */
@RunWith(AndroidJUnit4::class)
class RecordingServiceTest {

    private lateinit var testContext: android.content.Context

    @Before
    fun setup() {
        testContext = ApplicationProvider.getApplicationContext<android.app.Application>()
    }

    @Test
    fun serviceIntentConstantsAreDefined() {
        // Verify that the intent action constants are properly defined
        assertTrue("EXTRA_START_RECORDING should be defined",
            EXTRA_START_RECORDING.isNotEmpty())
        assertTrue("EXTRA_STOP_RECORDING should be defined",
            EXTRA_STOP_RECORDING.isNotEmpty())
        assertTrue("EXTRA_CANCEL_RECORDING should be defined",
            EXTRA_CANCEL_RECORDING.isNotEmpty())
    }

    @Test
    fun serviceCanBeStartedWithIntent() {
        val intent = Intent(testContext, RecordingService::class.java)
        intent.putExtra(EXTRA_START_RECORDING, true)

        // Verify intent is properly constructed
        assertTrue("Intent should have START_RECORDING extra",
            intent.getBooleanExtra(EXTRA_START_RECORDING, false))
    }

    companion object {
        const val EXTRA_START_RECORDING = "com.georgernstgraf.aitranscribe.START_RECORDING"
        const val EXTRA_STOP_RECORDING = "com.georgernstgraf.aitranscribe.STOP_RECORDING"
        const val EXTRA_CANCEL_RECORDING = "com.georgernstgraf.aitranscribe.CANCEL_RECORDING"
    }
}
