package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

class ShareTranscriptionUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: ShareTranscriptionUseCase

    @Before
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
        val id = repository.insert(testEntity(originalText = "Hello world"))

        val intent = useCase(id)

        assertNotNull(intent)
    }

    @Test
    fun `repository returns correct text for sharing`() = runBlocking {
        val id = repository.insert(testEntity(originalText = "Hello world"))

        val entity = repository.getById(id)
        assertNotNull(entity)
        assertEquals("Hello world", entity?.originalText)
    }

    @Test
    fun `repository returns processed text when available`() = runBlocking {
        val id = repository.insert(testEntity(
            originalText = "raw text",
            processedText = "cleaned text"
        ))

        val entity = repository.getById(id)
        assertNotNull(entity)
        assertEquals("cleaned text", entity?.processedText)
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
        originalText: String = "Test",
        processedText: String? = null
    ) = TranscriptionEntity(
        id = 0,
        originalText = originalText,
        processedText = processedText,
        audioFilePath = null,
        createdAt = LocalDateTime.now().toString(),
        status = "COMPLETED",
        errorMessage = null,
        seen = false
    )
}
