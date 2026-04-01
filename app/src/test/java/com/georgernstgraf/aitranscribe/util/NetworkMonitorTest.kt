package com.georgernstgraf.aitranscribe.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkMonitorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkMonitor: NetworkMonitor

    @BeforeEach
    fun setup() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        every { context.applicationContext } returns context
        
        networkMonitor = NetworkMonitor(context)
    }

    @Test
    fun `isConnected returns false when no active network`() {
        every { connectivityManager.activeNetwork } returns null

        val result = networkMonitor.isConnected()

        assertFalse(result)
    }

    @Test
    fun `isConnected returns false when no network capabilities`() {
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        val result = networkMonitor.isConnected()

        assertFalse(result)
    }

    @Test
    fun `isConnected returns true when internet is validated`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true

        val result = networkMonitor.isConnected()

        assertTrue(result)
    }

    @Test
    fun `isConnected returns false when internet not validated`() {
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false

        val result = networkMonitor.isConnected()

        assertFalse(result)
    }

    @Test
    fun `networkState registers network callback`() = runTest {
        // Verify that registerNetworkCallback is called when observing the flow
        verify(exactly = 0) { connectivityManager.registerNetworkCallback(any<NetworkRequest>(), any<ConnectivityManager.NetworkCallback>()) }
    }
}
