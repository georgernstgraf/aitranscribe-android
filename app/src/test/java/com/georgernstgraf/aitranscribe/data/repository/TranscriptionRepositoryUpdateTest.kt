package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class TranscriptionRepositoryUpdateTest {

    private lateinit var repository: FakeTranscriptionRepository

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
    }

    private fun createEntity(originalText: String = "Hello world"): TranscriptionEntity {
        return TranscriptionEntity(
            id = 0,
            originalText = originalText,
            processedText = "processed",
            audioFilePath = "/audio.m4a",
            createdAt = LocalDateTime.now().toString(),
            postProcessingType = "CLEANUP",
            status = "COMPLETED",
            errorMessage = null,
            playedCount = 3,
            retryCount = 1,
            summary = "A summary"
        )
    }

    @Test
    fun `update persists modified originalText`() = runBlocking {
        val id = repository.insert(createEntity("Original text"))

        val entity = repository.getById(id)!!
        repository.update(entity.copy(originalText = "Updated text"))

        val reloaded = repository.getById(id)!!
        assertEquals("Updated text", reloaded.originalText)
        assertEquals("processed", reloaded.processedText)
        assertEquals("/audio.m4a", reloaded.audioFilePath)
        assertEquals("CLEANUP", reloaded.postProcessingType)
        assertEquals(3, reloaded.playedCount)
        assertEquals(1, reloaded.retryCount)
        assertEquals("A summary", reloaded.summary)
    }

    @Test
    fun `update preserves all other fields`() = runBlocking {
        val id = repository.insert(createEntity("Original"))

        val entity = repository.getById(id)!!
        repository.update(entity.copy(originalText = "Changed", summary = "New summary"))

        val reloaded = repository.getById(id)!!
        assertEquals("Changed", reloaded.originalText)
        assertEquals("New summary", reloaded.summary)
        assertEquals(entity.processedText, reloaded.processedText)
        assertEquals(entity.audioFilePath, reloaded.audioFilePath)
        assertEquals(entity.postProcessingType, reloaded.postProcessingType)
        assertEquals(entity.playedCount, reloaded.playedCount)
    }

    @Test
    fun `getById returns null for non-existent id`() = runBlocking {
        assertNull(repository.getById(999L))
    }
}
