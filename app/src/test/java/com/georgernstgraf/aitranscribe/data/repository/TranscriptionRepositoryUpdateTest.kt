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

    private fun createEntity(text: String = "Hello world"): TranscriptionEntity {
        return TranscriptionEntity(
            id = 0,
            text = text,
            audioFilePath = "/audio.m4a",
            createdAt = LocalDateTime.now().toString(),
            status = "COMPLETED",
            errorMessage = null,
            seen = true,
            summary = "A summary"
        )
    }

    @Test
    fun `update persists modified text`() = runBlocking {
        val id = repository.insert(createEntity("Original text"))

        val entity = repository.getById(id)!!
        repository.update(entity.copy(text = "Updated text"))

        val reloaded = repository.getById(id)!!
        assertEquals("Updated text", reloaded.text)
        assertEquals("/audio.m4a", reloaded.audioFilePath)
        assertEquals(true, reloaded.seen)
        assertEquals("A summary", reloaded.summary)
    }

    @Test
    fun `update preserves all other fields`() = runBlocking {
        val id = repository.insert(createEntity("Original"))

        val entity = repository.getById(id)!!
        repository.update(entity.copy(text = "Changed", summary = "New summary"))

        val reloaded = repository.getById(id)!!
        assertEquals("Changed", reloaded.text)
        assertEquals("New summary", reloaded.summary)
        assertEquals(entity.audioFilePath, reloaded.audioFilePath)
        assertEquals(entity.seen, reloaded.seen)
    }

    @Test
    fun `getById returns null for non-existent id`() = runBlocking {
        assertNull(repository.getById(999L))
    }
}
