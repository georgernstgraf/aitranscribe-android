package com.georgernstgraf.aitranscribe.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TranscriptionTest {

    @Test
    fun `getShareText prepends summary when present`() {
        val t = testTranscription(
            originalText = "raw text",
            processedText = "cleaned text",
            summary = "A brief summary"
        )
        assertEquals("A brief summary: cleaned text", t.getShareText())
    }

    @Test
    fun `getShareText omits summary when null`() {
        val t = testTranscription(
            originalText = "raw text",
            processedText = "cleaned text",
            summary = null
        )
        assertEquals("cleaned text", t.getShareText())
    }

    @Test
    fun `getShareText omits summary when blank`() {
        val t = testTranscription(
            originalText = "raw text",
            processedText = null,
            summary = "   "
        )
        assertEquals("raw text", t.getShareText())
    }

    @Test
    fun `getShareText falls back to originalText when processedText is null`() {
        val t = testTranscription(
            originalText = "raw text",
            processedText = null,
            summary = "Summary here"
        )
        assertEquals("Summary here: raw text", t.getShareText())
    }

    @Test
    fun `getShareText uses processedText over originalText`() {
        val t = testTranscription(
            originalText = "raw",
            processedText = "processed",
            summary = null
        )
        assertEquals("processed", t.getShareText())
    }

    @Test
    fun `getShareText with empty summary falls back to text only`() {
        val t = testTranscription(
            originalText = "raw text",
            processedText = null,
            summary = ""
        )
        assertEquals("raw text", t.getShareText())
    }

    private fun testTranscription(
        originalText: String = "Test",
        processedText: String? = null,
        summary: String? = null
    ) = Transcription(
        id = 1L,
        originalText = originalText,
        processedText = processedText,
        audioFilePath = null,
        createdAt = LocalDateTime.now(),
        postProcessingType = null,
        status = TranscriptionStatus.COMPLETED,
        errorMessage = null,
        summary = summary
    )
}
