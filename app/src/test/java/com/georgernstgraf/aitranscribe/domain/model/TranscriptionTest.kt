package com.georgernstgraf.aitranscribe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TranscriptionTest {

    @Test
    fun `getShareText returns cleanedText when present`() {
        val t = testTranscription(
            sttText = "raw text",
            cleanedText = "cleaned text",
            summary = "A brief summary"
        )
        assertEquals("cleaned text", t.getShareText())
    }

    @Test
    fun `getShareText returns sttText when cleanedText is null`() {
        val t = testTranscription(
            sttText = "raw text",
            cleanedText = null,
            summary = null
        )
        assertEquals("raw text", t.getShareText())
    }

    @Test
    fun `getShareText returns sttText when cleanedText is blank`() {
        val t = testTranscription(
            sttText = "raw text",
            cleanedText = "   ",
            summary = "   "
        )
        assertEquals("raw text", t.getShareText())
    }

    @Test
    fun `getShareText returns empty string when both texts are null`() {
        val t = testTranscription(
            sttText = null,
            cleanedText = null,
            summary = "Summary here"
        )
        assertEquals("", t.getShareText())
    }

    @Test
    fun `getShareText returns cleanedText over sttText when not blank`() {
        val t = testTranscription(
            sttText = "raw text",
            cleanedText = "processed",
            summary = null
        )
        assertEquals("processed", t.getShareText())
    }

    @Test
    fun `getShareText with empty cleanedText falls back to sttText`() {
        val t = testTranscription(
            sttText = "raw text",
            cleanedText = "",
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

    @Test
    fun `displayText returns cleanedText when present and not blank`() {
        val t = testTranscription(sttText = "raw", cleanedText = "cleaned")
        assertEquals("cleaned", t.displayText)
    }

    @Test
    fun `displayText returns sttText when cleanedText is null`() {
        val t = testTranscription(sttText = "raw", cleanedText = null)
        assertEquals("raw", t.displayText)
    }

    @Test
    fun `displayText returns sttText when cleanedText is blank`() {
        val t = testTranscription(sttText = "raw", cleanedText = "   ")
        assertEquals("raw", t.displayText)
    }

    @Test
    fun `displayText returns null when both are null`() {
        val t = testTranscription(sttText = null, cleanedText = null)
        assertEquals(null, t.displayText)
    }

    @Test
    fun `isPending returns true when sttText is null and audioFilePath is not null`() {
        val t = testTranscription(sttText = null, audioFilePath = "/audio.mp3")
        assertEquals(true, t.isPending)
    }

    @Test
    fun `isPending returns false when sttText is not null`() {
        val t = testTranscription(sttText = "some text", audioFilePath = "/audio.mp3")
        assertEquals(false, t.isPending)
    }

    @Test
    fun `isPending returns false when audioFilePath is null`() {
        val t = testTranscription(sttText = null, audioFilePath = null)
        assertEquals(false, t.isPending)
    }

    private fun testTranscription(
        sttText: String? = "Test",
        cleanedText: String? = null,
        audioFilePath: String? = null,
        summary: String? = null
    ) = Transcription(
        id = 1L,
        sttText = sttText,
        cleanedText = cleanedText,
        audioFilePath = audioFilePath,
        createdAt = LocalDateTime.now(),
        errorMessage = null,
        seen = false,
        summary = summary,
        language = null
    )
}
