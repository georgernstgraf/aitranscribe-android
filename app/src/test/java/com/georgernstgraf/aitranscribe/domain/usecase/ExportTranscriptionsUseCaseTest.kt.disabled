package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDateTime

/**
 * Test class for ExportTranscriptionsUseCase.
 * Tests transcription export functionality.
 */
class ExportTranscriptionsUseCaseTest {

    private lateinit var useCase: ExportTranscriptionsUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository
    private lateinit var testDirectory: java.nio.file.Path

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        testDirectory = java.nio.file.Files.createTempDirectory("aitranscribe", "export")
        useCase = ExportTranscriptionsUseCase(fakeRepository, testDirectory.toString())
    }

    @Test
    fun `export creates JSON file`() = runTest {
        val id1 = createTestTranscription(id = 1)
        val id2 = createTestTranscription(id = 2)

        val outputFile = useCase.invoke(format = "json")

        assertTrue("Export file should be created", File(outputFile).exists())
        assertTrue("Should be JSON file", outputFile.endsWith(".json"))
    }

    @Test
    fun `export creates CSV file`() = runTest {
        val id = createTestTranscription(id = 1)

        val outputFile = useCase.invoke(format = "csv")

        assertTrue("Export file should be created", File(outputFile).exists())
        assertTrue("Should be CSV file", outputFile.endsWith(".csv"))
    }

    @Test
    fun `export filters by date range`() = runTest {
        val now = LocalDateTime.now()
        createTestTranscription(id = 1, createdAt = now.minusDays(40))
        createTestTranscription(id = 2, createdAt = now.minusDays(10))
        createTestTranscription(id = 3, createdAt = now)

        val outputFile = useCase.invoke(
            format = "json",
            startDate = now.minusDays(30).toString(),
            endDate = now.plusDays(1).toString()
        )

        val file = File(outputFile)
        assertTrue("Export file should be created", file.exists())
        assertTrue("Should contain recent transcriptions", file.readText().contains("id 2"))
    }

    @Test
    fun `export filters by view status`() = runTest {
        createTestTranscription(id = 1, playedCount = 0)
        createTestTranscription(id = 2, playedCount = 1)
        createTestTranscription(id = 3, playedCount = 0)

        val outputFile = useCase.invoke(
            format = "json",
            viewFilter = com.georgernstgraf.aitranscribe.domain.model.ViewFilter.UNVIEWED_ONLY
        )

        val file = File(outputFile)
        assertTrue("Export file should be created", file.exists())
        val content = file.readText()
        assertTrue("Should contain unviewed", content.contains("id 1"))
        assertTrue("Should contain unviewed", content.contains("id 3"))
    }

    @Test
    fun `export includes all fields`() = runTest {
        val id = createTestTranscription(
            originalText = "Test text",
            processedText = "Processed text",
            playedCount = 1
        )

        val outputFile = useCase.invoke(format = "json")

        val file = File(outputFile)
        val content = file.readText()
        assertTrue("Should include original text", content.contains("Test text"))
        assertTrue("Should include processed text", content.contains("Processed text"))
        assertTrue("Should include played count", content.contains("played_count"))
    }

    @Test(expected = ExportException::class)
    fun `export throws exception for invalid format`() = runTest {
        useCase.invoke(format = "invalid")
    }

    @Test
    fun `export throws exception for empty repository`() = runTest {
        try {
            useCase.invoke(format = "json")
        } catch (e: ExportException) {
            assertTrue("Should mention empty repository", e.message?.contains("No transcriptions"))
            throw e
        }
    }

    private fun createTestTranscription(
        id: Long,
        playedCount: Int = 0,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Long {
        val entity = com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity(
            id = id,
            originalText = "Test transcription $id",
            processedText = if (id % 2 == 0L) "Processed $id" else null,
            audioFilePath = "/path/to/audio$id.mp3",
            createdAt = createdAt.toString(),
            postProcessingType = null,
            status = com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
        return fakeRepository.insert(entity)
    }

    class ExportException(message: String) : Exception(message)
}