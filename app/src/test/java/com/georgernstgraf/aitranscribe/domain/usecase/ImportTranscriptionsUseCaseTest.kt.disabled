package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Test class for ImportTranscriptionsUseCase.
 * Tests transcription import functionality.
 */
class ImportTranscriptionsUseCaseTest {

    private lateinit var useCase: ImportTranscriptionsUseCase
    private lateinit var fakeRepository: FakeTranscriptionRepository
    private lateinit var testFile: File

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        useCase = ImportTranscriptionsUseCase(fakeRepository)
        testFile = createTempJsonFile()
    }

    @Test
    fun `import from JSON creates transcriptions`() = runTest {
        val count = useCase.invoke(testFile.absolutePath)

        assertEquals("Should import 3 transcriptions", 3, count)
        assertEquals("Should have 3 items in repository", 3, fakeRepository.getCount())
    }

    @Test
    fun `import preserves transcription fields`() = runTest {
        useCase.invoke(testFile.absolutePath)

        val transcription = fakeRepository.getById(1)
        assertNotNull("Transcription should be imported", transcription)
        assertEquals("Should preserve ID", 1, transcription?.id)
        assertTrue("Should have original text", transcription?.originalText?.isNotEmpty() ?: false)
    }

    @Test
    fun `import handles empty JSON`() = runTest {
        val emptyFile = File.createTempFile("empty", ".json")
        emptyFile.writeText("[]")

        val count = useCase.invoke(emptyFile.absolutePath)

        assertEquals("Should import 0 transcriptions", 0, count)
    }

    @Test(expected = ImportException::class)
    fun `import throws exception for non-existent file`() = runTest {
        useCase.invoke("/non/existent/file.json")
    }

    @Test(expected = ImportException::class)
    fun `import throws exception for invalid JSON`() = runTest {
        val invalidFile = File.createTempFile("invalid", ".json")
        invalidFile.writeText("{ invalid json")

        useCase.invoke(invalidFile.absolutePath)
    }

    @Test
    fun `import handles CSV format`() = runTest {
        val csvFile = createTempCsvFile()

        val count = useCase.invoke(csvFile.absolutePath)

        assertEquals("Should import 2 transcriptions", 2, count)
    }

    @Test
    fun `import handles duplicate IDs`() = runTest {
        useCase.invoke(testFile.absolutePath)
        val count1 = fakeRepository.getCount()

        useCase.invoke(testFile.absolutePath)
        val count2 = fakeRepository.getCount()

        assertEquals("Should not create duplicates", count1, count2)
    }

    private fun createTempJsonFile(): File {
        val file = File.createTempFile("test_import", ".json")
        file.writeText("""
            [
                {
                    "id": 1,
                    "originalText": "Test transcription 1",
                    "processedText": "Processed 1",
                    "audioFilePath": "/path/to/audio1.mp3",
                    "createdAt": "2024-03-01T10:00:00",
                    "postProcessingType": null,
                    "status": "COMPLETED",
                    "errorMessage": null,
                    "playedCount": 0,
                    "retryCount": 0
                },
                {
                    "id": 2,
                    "originalText": "Test transcription 2",
                    "processedText": null,
                    "audioFilePath": "/path/to/audio2.mp3",
                    "createdAt": "2024-03-01T11:00:00",
                    "postProcessingType": null,
                    "status": "COMPLETED",
                    "errorMessage": null,
                    "playedCount": 1,
                    "retryCount": 0
                },
                {
                    "id": 3,
                    "originalText": "Test transcription 3",
                    "processedText": "Processed 3",
                    "audioFilePath": "/path/to/audio3.mp3",
                    "createdAt": "2024-03-01T12:00:00",
                    "postProcessingType": "GRAMMAR",
                    "status": "COMPLETED",
                    "errorMessage": null,
                    "playedCount": 0,
                    "retryCount": 0
                }
            ]
        """.trimIndent())
        return file
    }

    private fun createTempCsvFile(): File {
        val file = File.createTempFile("test_import", ".csv")
        file.writeText("""
            ID,Original Text,Processed Text,Created At,Status,Played Count
            1,"Test 1","Processed 1","2024-03-01T10:00:00",COMPLETED,0
            2,"Test 2","","2024-03-01T11:00:00",COMPLETED,1
        """.trimIndent())
        return file
    }

    class ImportException(message: String) : Exception(message)
}