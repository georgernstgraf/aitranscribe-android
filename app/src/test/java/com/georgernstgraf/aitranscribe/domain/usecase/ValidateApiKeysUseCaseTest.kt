package com.georgernstgraf.aitranscribe.domain.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateApiKeysUseCaseTest {

    private lateinit var useCase: ValidateApiKeysUseCase

    @Before
    fun setup() {
        useCase = ValidateApiKeysUseCase(okhttp3.OkHttpClient())
    }

    @Test
    fun `both keys missing returns MISSING errors`() = runBlocking {
        val result = useCase(null, null)

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.MISSING, result.groqKeyError)
        assertEquals(ApiKeyError.MISSING, result.openRouterKeyError)
    }

    @Test
    fun `groq key missing returns MISSING groq error`() = runBlocking {
        val result = useCase(null, "sk-or-validkey1234567890")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.MISSING, result.groqKeyError)
        assertNull(result.openRouterKeyError)
    }

    @Test
    fun `openrouter key missing returns MISSING openrouter error`() = runBlocking {
        val result = useCase("gsk_validkey1234567890123", null)

        assertFalse(result.isValid)
        assertNull(result.groqKeyError)
        assertEquals(ApiKeyError.MISSING, result.openRouterKeyError)
    }

    @Test
    fun `invalid groq key format returns INVALID_FORMAT`() = runBlocking {
        val result = useCase("short", "sk-or-validkey1234567890")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.INVALID_FORMAT, result.groqKeyError)
    }

    @Test
    fun `invalid openrouter key format returns INVALID_FORMAT`() = runBlocking {
        val result = useCase("gsk_validkey1234567890123", "short")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.INVALID_FORMAT, result.openRouterKeyError)
    }

    @Test
    fun `groq key must start with gsk_`() = runBlocking {
        val result = useCase("invalid_prefix_12345678901234", "sk-or-validkey1234567890")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.INVALID_FORMAT, result.groqKeyError)
    }

    @Test
    fun `openrouter key must start with sk-or- or sk-`() = runBlocking {
        val result = useCase("gsk_validkey1234567890123", "invalid_prefix_12345678901234")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.INVALID_FORMAT, result.openRouterKeyError)
    }

    @Test
    fun `groq key too short returns INVALID_FORMAT`() = runBlocking {
        val result = useCase("gsk_short", "sk-or-validkey1234567890")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.INVALID_FORMAT, result.groqKeyError)
    }

    @Test
    fun `openrouter key too short returns INVALID_FORMAT`() = runBlocking {
        val result = useCase("gsk_validkey1234567890123", "sk-short")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.INVALID_FORMAT, result.openRouterKeyError)
    }

    @Test
    fun `blank keys treated as missing`() = runBlocking {
        val result = useCase("   ", "   ")

        assertFalse(result.isValid)
        assertEquals(ApiKeyError.MISSING, result.groqKeyError)
        assertEquals(ApiKeyError.MISSING, result.openRouterKeyError)
    }

    @Test
    fun `sk-prefix passes format validation for openrouter key`() = runBlocking {
        val result = useCase("gsk_validkey1234567890123", "sk-validkey12345678901234")

        assertFalse(result.isValid)
        assertNotEqualsFormatError(result.openRouterKeyError)
    }

    private fun assertNotEqualsFormatError(error: ApiKeyError?) {
        assertTrue(
            "Expected non-format error but got: $error",
            error != ApiKeyError.INVALID_FORMAT
        )
    }
}
