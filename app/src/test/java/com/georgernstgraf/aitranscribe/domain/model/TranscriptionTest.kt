package com.georgernstgraf.aitranscribe.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class TranscriptionTest {

    @Test
    fun `getShareText omits summary when present`() {
        val t = testTranscription(
            originalText = "raw text",
            processedText = "cleaned text",
            summary = "A brief summary"
        )
        assertEquals("cleaned text", t.getShareText())
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
        assertEquals("raw text", t.getShareText())
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

    @Test
    fun `getShareTitle uses summary when present`() {
        val t = testTranscription(summary = "My Meeting Notes")
        assertEquals("My Meeting Notes", t.getShareTitle())
    }

    @Test
    fun `getShareTitle defaults when summary is null`() {
        val t = testTranscription(summary = null)
        assertEquals("Transcription from AITranscribe", t.getShareTitle())
    }

    @Test
    fun `getShareTitle defaults when summary is blank`() {
        val t = testTranscription(summary = "   ")
        assertEquals("Transcription from AITranscribe", t.getShareTitle())
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
