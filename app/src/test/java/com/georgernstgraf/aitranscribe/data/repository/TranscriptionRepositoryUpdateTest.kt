package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TranscriptionRepositoryUpdateTest {

    private lateinit var repository: FakeTranscriptionRepository

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
    }

    private fun createEntity(sttText: String = "Hello world", cleanedText: String? = null): TranscriptionEntity {
        return TranscriptionEntity(
            id = 0,
            sttText = sttText,
            cleanedText = cleanedText,
            audioFilePath = "/audio.m4a",
            createdAt = LocalDateTime.now().toString(),
            errorMessage = null,
            seen = true,
            summary = "A summary"
        )
    }

    @Test
    fun `update persists modified sttText`() = runBlocking {
        val id = repository.insert(createEntity("Original stt text"))

        val entity = repository.getById(id)!!
        repository.update(entity.copy(sttText = "Updated stt text"))

        val reloaded = repository.getById(id)!!
        assertEquals("Updated stt text", reloaded.sttText)
        assertEquals("/audio.m4a", reloaded.audioFilePath)
        assertEquals(true, reloaded.seen)
        assertEquals("A summary", reloaded.summary)
    }

    @Test
    fun `update persists modified cleanedText`() = runBlocking {
        val id = repository.insert(createEntity("Original stt", "Original cleaned"))

        val entity = repository.getById(id)!!
        repository.update(entity.copy(cleanedText = "Updated cleaned text"))

        val reloaded = repository.getById(id)!!
        assertEquals("Updated cleaned text", reloaded.cleanedText)
    }

    @Test
    fun `update preserves all other fields`() = runBlocking {
        val id = repository.insert(createEntity("Original stt", "Original cleaned"))

        val entity = repository.getById(id)!!
        repository.update(entity.copy(sttText = "Changed stt", summary = "New summary"))

        val reloaded = repository.getById(id)!!
        assertEquals("Changed stt", reloaded.sttText)
        assertEquals("Original cleaned", reloaded.cleanedText)
        assertEquals("New summary", reloaded.summary)
        assertEquals(entity.audioFilePath, reloaded.audioFilePath)
        assertEquals(entity.seen, reloaded.seen)
    }

    @Test
    fun `getById returns null for non-existent id`() = runBlocking {
        assertNull(repository.getById(999L))
    }
}
