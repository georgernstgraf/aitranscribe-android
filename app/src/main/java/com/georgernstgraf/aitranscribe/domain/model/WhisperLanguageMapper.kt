package com.georgernstgraf.aitranscribe.domain.model

object WhisperLanguageMapper {

    private val nameToCode: Map<String, String> = mapOf(
        "german" to "de",
        "english" to "en",
        "french" to "fr",
        "spanish" to "es",
        "italian" to "it",
        "portuguese" to "pt",
        "dutch" to "nl",
        "polish" to "pl",
        "russian" to "ru",
        "japanese" to "ja",
        "chinese" to "zh",
        "korean" to "ko",
        "arabic" to "ar",
        "hindi" to "hi",
        "turkish" to "tr",
        "swedish" to "sv",
        "danish" to "da",
        "norwegian" to "no",
        "finnish" to "fi",
        "czech" to "cs",
        "hungarian" to "hu",
        "romanian" to "ro",
        "greek" to "el",
        "hebrew" to "he",
        "thai" to "th",
        "vietnamese" to "vi",
        "indonesian" to "id",
        "malay" to "ms",
        "ukrainian" to "uk",
        "bulgarian" to "bg",
        "croatian" to "hr",
        "serbian" to "sr",
        "slovak" to "sk",
        "slovenian" to "sl",
        "lithuanian" to "lt",
        "latvian" to "lv",
        "estonian" to "et"
    )

    fun mapToCode(whisperLanguageName: String): String {
        return nameToCode[whisperLanguageName.lowercase()] ?: whisperLanguageName
    }
}
