package com.georgernstgraf.aitranscribe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WhisperLanguageMapperTest {

    @Test
    fun `maps all 37 prepopulated languages`() {
        assertEquals("de", WhisperLanguageMapper.mapToCode("german"))
        assertEquals("en", WhisperLanguageMapper.mapToCode("english"))
        assertEquals("fr", WhisperLanguageMapper.mapToCode("french"))
        assertEquals("es", WhisperLanguageMapper.mapToCode("spanish"))
        assertEquals("it", WhisperLanguageMapper.mapToCode("italian"))
        assertEquals("pt", WhisperLanguageMapper.mapToCode("portuguese"))
        assertEquals("nl", WhisperLanguageMapper.mapToCode("dutch"))
        assertEquals("pl", WhisperLanguageMapper.mapToCode("polish"))
        assertEquals("ru", WhisperLanguageMapper.mapToCode("russian"))
        assertEquals("ja", WhisperLanguageMapper.mapToCode("japanese"))
        assertEquals("zh", WhisperLanguageMapper.mapToCode("chinese"))
        assertEquals("ko", WhisperLanguageMapper.mapToCode("korean"))
        assertEquals("ar", WhisperLanguageMapper.mapToCode("arabic"))
        assertEquals("hi", WhisperLanguageMapper.mapToCode("hindi"))
        assertEquals("tr", WhisperLanguageMapper.mapToCode("turkish"))
        assertEquals("sv", WhisperLanguageMapper.mapToCode("swedish"))
        assertEquals("da", WhisperLanguageMapper.mapToCode("danish"))
        assertEquals("no", WhisperLanguageMapper.mapToCode("norwegian"))
        assertEquals("fi", WhisperLanguageMapper.mapToCode("finnish"))
        assertEquals("cs", WhisperLanguageMapper.mapToCode("czech"))
        assertEquals("hu", WhisperLanguageMapper.mapToCode("hungarian"))
        assertEquals("ro", WhisperLanguageMapper.mapToCode("romanian"))
        assertEquals("el", WhisperLanguageMapper.mapToCode("greek"))
        assertEquals("he", WhisperLanguageMapper.mapToCode("hebrew"))
        assertEquals("th", WhisperLanguageMapper.mapToCode("thai"))
        assertEquals("vi", WhisperLanguageMapper.mapToCode("vietnamese"))
        assertEquals("id", WhisperLanguageMapper.mapToCode("indonesian"))
        assertEquals("ms", WhisperLanguageMapper.mapToCode("malay"))
        assertEquals("uk", WhisperLanguageMapper.mapToCode("ukrainian"))
        assertEquals("bg", WhisperLanguageMapper.mapToCode("bulgarian"))
        assertEquals("hr", WhisperLanguageMapper.mapToCode("croatian"))
        assertEquals("sr", WhisperLanguageMapper.mapToCode("serbian"))
        assertEquals("sk", WhisperLanguageMapper.mapToCode("slovak"))
        assertEquals("sl", WhisperLanguageMapper.mapToCode("slovenian"))
        assertEquals("lt", WhisperLanguageMapper.mapToCode("lithuanian"))
        assertEquals("lv", WhisperLanguageMapper.mapToCode("latvian"))
        assertEquals("et", WhisperLanguageMapper.mapToCode("estonian"))
    }

    @Test
    fun `is case insensitive`() {
        assertEquals("en", WhisperLanguageMapper.mapToCode("English"))
        assertEquals("de", WhisperLanguageMapper.mapToCode("GERMAN"))
        assertEquals("fr", WhisperLanguageMapper.mapToCode("FreNch"))
    }

    @Test
    fun `returns input unchanged for unknown language`() {
        assertEquals("klingon", WhisperLanguageMapper.mapToCode("klingon"))
    }

    @Test
    fun `returns input unchanged for bcp-47 code passed directly`() {
        assertEquals("en", WhisperLanguageMapper.mapToCode("en"))
    }
}
