package com.georgernstgraf.aitranscribe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TranscriptionTest {

    @Test
    fun `getShareText omits summary when present`() {
        val t = testTranscription(
            text = "raw text",
            textOverride = "cleaned text",
            summary = "A brief summary"
        )
        assertEquals("cleaned text", t.getShareText())
    }

    @Test
    fun `getShareText omits summary when null`() {
        val t = testTranscription(
            textOverride = "cleaned text",
            summary = null
        )
        assertEquals("cleaned text", t.getShareText())
    }

    @Test
    fun `getShareText omits summary when blank`() {
        val t = testTranscription(
            text = "raw text",
            summary = "   "
        )
        assertEquals("raw text", t.getShareText())
    }

    @Test
    fun `getShareText returns text`() {
        val t = testTranscription(
            text = "raw text",
            summary = "Summary here"
        )
        assertEquals("raw text", t.getShareText())
    }

    @Test
    fun `getShareText uses updated text`() {
        val t = testTranscription(
            textOverride = "processed",
            summary = null
        )
        assertEquals("processed", t.getShareText())
    }

    @Test
    fun `getShareText with empty summary falls back to text only`() {
        val t = testTranscription(
            text = "raw text",
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
        text: String = "Test",
        textOverride: String? = null,
        summary: String? = null
    ) = Transcription(
        id = 1L,
        text = textOverride ?: text,
        audioFilePath = null,
        createdAt = LocalDateTime.now(),
        status = TranscriptionStatus.COMPLETED,
        errorMessage = null,
        summary = summary
    )
}
