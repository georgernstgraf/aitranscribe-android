package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDateTime

class ImportTranscriptionsUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: ImportTranscriptionsUseCase
    private lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = ImportTranscriptionsUseCase(repository)
        tempDir = File(System.getProperty("java.io.tmpdir"), "aitranscribe_test")
        tempDir.mkdirs()
    }

    @Test
    fun `invoke imports transcriptions from JSON file`() = runTest {
        val jsonFile = File(tempDir, "test_import.json")
        jsonFile.writeText("""
            [
                {
                    "id": 1,
                    "text": "Hello world",
                    "processedText": null,
                    "audioFilePath": "/test.mp3",
                    "createdAt": "2026-03-23T20:00:00",
                    "postProcessingType": null,
                    "status": "COMPLETED",
                    "errorMessage": null,
                    "playedCount": 0,
                    "retryCount": 0
                }
            ]
        """.trimIndent())

        val result = useCase(jsonFile.absolutePath)

        assertEquals(1, result)
        jsonFile.delete()
    }

    @Test
    fun `invoke throws exception for non-existent file`() {
        assertThrows(ImportTranscriptionsUseCase.ImportException::class.java) {
            runTest { useCase("/non/existent/file.json") }
        }
    }
}
