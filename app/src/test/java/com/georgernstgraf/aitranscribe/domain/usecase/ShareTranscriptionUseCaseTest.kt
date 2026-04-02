package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDateTime

class ShareTranscriptionUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: ShareTranscriptionUseCase

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        val context = mockk<android.content.Context>(relaxed = true)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "share_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        every { context.cacheDir } returns tempDir
        every { context.packageName } returns "com.georgernstgraf.aitranscribe.debug"
        useCase = ShareTranscriptionUseCase(context, repository)
    }

    @Test
    fun `invoke returns non-null intent for existing transcription`() = runBlocking {
        val id = repository.insert(testEntity(sttText = "Hello world"))

        val intent = useCase(id)

        assertNotNull(intent)
    }

    @Test
    fun `repository returns correct sttText`() = runBlocking {
        val id = repository.insert(testEntity(sttText = "Hello world"))

        val entity = repository.getById(id)
        assertNotNull(entity)
        assertEquals("Hello world", entity?.sttText)
    }

    @Test
    fun `repository returns cleanedText when present`() = runBlocking {
        val id = repository.insert(testEntity(sttText = "raw text", cleanedText = "cleaned text"))

        val entity = repository.getById(id)
        assertNotNull(entity)
        assertEquals("cleaned text", entity?.cleanedText)
    }

    @Test
    fun `invoke throws for non-existent transcription`() = runBlocking {
        try {
            useCase(999L)
            throw AssertionError("Expected ShareException")
        } catch (_: ShareTranscriptionUseCase.ShareException) {
            // expected
        }
    }

    @Test
    fun `invokeAsFile throws for non-existent transcription`() = runBlocking {
        try {
            useCase.invokeAsFile(999L)
            throw AssertionError("Expected ShareException")
        } catch (_: ShareTranscriptionUseCase.ShareException) {
            // expected
        }
    }

    private fun testEntity(
        sttText: String = "Test",
        cleanedText: String? = null
    ) = TranscriptionEntity(
        id = 0,
        sttText = sttText,
        cleanedText = cleanedText,
        audioFilePath = null,
        createdAt = LocalDateTime.now().toString(),
        errorMessage = null,
        seen = false
    )
}
