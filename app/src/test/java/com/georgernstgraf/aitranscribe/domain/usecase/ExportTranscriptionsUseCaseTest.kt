package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ExportTranscriptionsUseCaseTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var outputDir: String
    private lateinit var useCase: ExportTranscriptionsUseCase

    @Before
    fun setup() {
        repository = FakeTranscriptionRepository()
        outputDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        useCase = ExportTranscriptionsUseCase(repository, outputDir)
    }

    @Test
    fun `repository returns transcriptions for export`() = runTest {
        repository.insert(
            TranscriptionEntity(
                id = 0,
                originalText = "Test transcription",
                processedText = "Processed",
                audioFilePath = "/test.mp3",
                createdAt = LocalDateTime.now().toString(),
                postProcessingType = null,
                status = "COMPLETED",
                errorMessage = null,
                seen = false,
                retryCount = 0
            )
        )

        val transcriptions = repository.searchTranscriptions(
            startDate = null,
            endDate = null,
            searchQuery = null,
            viewFilter = ViewFilter.ALL
        ).first()

        assertTrue(transcriptions.isNotEmpty())
        assertTrue(transcriptions[0].originalText == "Test transcription")
    }
}
