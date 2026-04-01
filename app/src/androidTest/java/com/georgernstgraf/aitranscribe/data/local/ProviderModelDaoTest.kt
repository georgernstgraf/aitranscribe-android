package com.georgernstgraf.aitranscribe.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProviderModelDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: TranscriptionDatabase
    private lateinit var dao: ProviderModelDao

    @BeforeEach
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TranscriptionDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.providerModelDao()
    }

    @AfterEach
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
        assertEquals("Provider 1", single?.name)
    }

    @Test
    fun replaceModelsForProvider_cascadesAndUpdatesTimestamp() = runBlocking {
        dao.insertProviders(listOf(ProviderEntity("prov1", "Prov 1", 0L)))
        
        val initialModels = listOf(
            ModelCatalogEntry(
                externalId = "mod1",
                modelName = "Model 1",
                capabilities = emptyList()
            ),
            ModelCatalogEntry(
                externalId = "mod2",
                modelName = "Model 2",
                capabilities = emptyList()
            )
        )
        dao.replaceModelsForProvider("prov1", initialModels, 500L)
        
        var currentModels = dao.getModelsForProvider("prov1")
        assertEquals(2, currentModels.size)
        
        var prov = dao.getProviderById("prov1")
        assertEquals(500L, prov?.lastSyncedAt)
        
        val newModels = listOf(
            ModelCatalogEntry(
                externalId = "mod3",
                modelName = "Model 3",
                capabilities = emptyList()
            )
        )
        dao.replaceModelsForProvider("prov1", newModels, 1000L)
        
        currentModels = dao.getModelsForProvider("prov1")
        assertEquals(1, currentModels.size)
        assertEquals("mod3", currentModels[0].externalId)
        
        prov = dao.getProviderById("prov1")
        assertEquals(1000L, prov?.lastSyncedAt)
    }
}
