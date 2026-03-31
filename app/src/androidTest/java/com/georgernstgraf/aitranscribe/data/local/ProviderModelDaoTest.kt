package com.georgernstgraf.aitranscribe.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProviderModelDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: TranscriptionDatabase
    private lateinit var dao: ProviderModelDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TranscriptionDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.providerModelDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetProviders() = runBlocking {
        val providers = listOf(
            ProviderEntity("provider1", "Provider 1", 1000L),
            ProviderEntity("provider2", "Provider 2", 2000L)
        )
        dao.insertProviders(providers)

        val retrieved = dao.getAllProviders()
        assertEquals(2, retrieved.size)
        assertTrue(retrieved.map { it.id }.containsAll(listOf("provider1", "provider2")))
        
        val single = dao.getProviderById("provider1")
        assertNotNull(single)
        assertEquals("Provider 1", single?.displayName)
    }

    @Test
    fun replaceModelsForProvider_cascadesAndUpdatesTimestamp() = runBlocking {
        dao.insertProviders(listOf(ProviderEntity("prov1", "Prov 1", 0L)))
        
        val initialModels = listOf(
            ModelEntity("mod1", "prov1", "Model 1"),
            ModelEntity("mod2", "prov1", "Model 2")
        )
        dao.replaceModelsForProvider("prov1", initialModels, 500L)
        
        var currentModels = dao.getModelsForProvider("prov1")
        assertEquals(2, currentModels.size)
        
        var prov = dao.getProviderById("prov1")
        assertEquals(500L, prov?.lastSyncedAt)
        
        val newModels = listOf(
            ModelEntity("mod3", "prov1", "Model 3")
        )
        dao.replaceModelsForProvider("prov1", newModels, 1000L)
        
        currentModels = dao.getModelsForProvider("prov1")
        assertEquals(1, currentModels.size)
        assertEquals("mod3", currentModels[0].id)
        
        prov = dao.getProviderById("prov1")
        assertEquals(1000L, prov?.lastSyncedAt)
    }
}
