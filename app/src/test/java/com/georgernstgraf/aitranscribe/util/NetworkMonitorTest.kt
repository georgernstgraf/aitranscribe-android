package com.georgernstgraf.aitranscribe.util

import com.georgernstgraf.aitranscribe.data.local.QueuedTranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Test class for NetworkMonitor.
 * Tests network connectivity monitoring.
 */
class NetworkMonitorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var fakeConnectivityManager: FakeConnectivityManager

    @Before
    fun setup() {
        fakeConnectivityManager = FakeConnectivityManager()
        networkMonitor = NetworkMonitor(fakeConnectivityManager)
    }

    @Test
    fun `network is connected when connectivity manager returns connected`() = runTest {
        fakeConnectivityManager.setConnected(true)

        val isConnected = networkMonitor.isConnected()

        assertTrue("Should be connected when manager returns true", isConnected)
    }

    @Test
    fun `network is not connected when connectivity manager returns disconnected`() = runTest {
        fakeConnectivityManager.setConnected(false)

        val isConnected = networkMonitor.isConnected()

        assertFalse("Should not be connected when manager returns false", isConnected)
    }

    @Test
    fun `network state changes emit values through flow`() = runTest {
        val states = mutableListOf<Boolean>()

        networkMonitor.networkState.collect { states.add(it) }

        fakeConnectivityManager.setConnected(true)
        fakeConnectivityManager.setConnected(false)
        fakeConnectivityManager.setConnected(true)

        assertEquals("Should emit network state changes", 3, states.size)
        assertEquals("First state should be true", true, states[0])
        assertEquals("Second state should be false", false, states[1])
        assertEquals("Third state should be true", true, states[2])
    }

    @Test
    fun `network state flow collects continuously`() = runTest {
        var lastState: Boolean? = null

        networkMonitor.networkState.collect { lastState = it }

        fakeConnectivityManager.setConnected(true)
        assertEquals("State should update", true, lastState)

        fakeConnectivityManager.setConnected(false)
        assertEquals("State should update again", false, lastState)
    }

    @Test
    fun `network monitor handles null network`() = runTest {
        fakeConnectivityManager.setConnected(false)

        val isConnected = networkMonitor.isConnected()

        assertFalse("Should handle null network gracefully", isConnected)
    }

    class FakeConnectivityManager {
        private var isConnected: Boolean = false

        fun setConnected(connected: Boolean) {
            isConnected = connected
        }

        fun getNetworkState(): Boolean = isConnected
    }
}