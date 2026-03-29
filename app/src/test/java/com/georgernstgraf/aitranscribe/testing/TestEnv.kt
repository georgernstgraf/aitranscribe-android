package com.georgernstgraf.aitranscribe.testing

import org.junit.Assert.fail
import java.io.File
import java.util.Properties

object TestEnv {

    private val env: Properties by lazy { loadEnv() }

    private val PLACEHOLDER_INDICATORS = listOf(
        "your_", "_here", "replace_", "fill_in", "TODO"
    )

    private fun loadEnv(): Properties {
        val props = Properties()

        System.getProperties().stringPropertyNames().forEach { key ->
            val value = System.getProperty(key)
            if (value != null) props[key] = value
        }

        val envFile = findEnvFile()
        if (envFile != null && envFile.exists()) {
            envFile.bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .filter { "=" in it }
                    .forEach { line ->
                        val idx = line.indexOf('=')
                        val key = line.substring(0, idx).trim()
                        val value = line.substring(idx + 1).trim()
                        if (key.isNotEmpty() && !props.containsKey(key)) {
                            props[key] = stripQuotes(value)
                        }
                    }
            }
        }
        return props
    }

    private fun stripQuotes(value: String): String {
        var v = value
        if (v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length - 1)
        if (v.startsWith("'") && v.endsWith("'")) v = v.substring(1, v.length - 1)
        return v
    }

    private fun findEnvFile(): File? {
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(8) {
            val candidate = File(dir, ".env")
            if (candidate.exists()) return candidate
            val parent = dir.parentFile ?: return null
            dir = parent
        }
        return null
    }

    fun getGroqApiKey(): String = requireNonPlaceholder("GROQ_API_KEY")

    fun getOpenRouterApiKey(): String = requireNonPlaceholder("OPENROUTER_API_KEY")

    fun getSttModel(): String = requireNonPlaceholder("GROQ_STT_MODEL")

    fun getLlmModel(): String = requireNonPlaceholder("OPENROUTER_LLM_MODEL")

    fun isApiIntegrationEnabled(): Boolean {
        return env.getProperty("RUN_API_INTEGRATION_TESTS", "false") == "true"
    }

    fun requireApiIntegration() {
        if (!isApiIntegrationEnabled()) {
            fail(
                "Set RUN_API_INTEGRATION_TESTS=true in .env to run this test.\n" +
                "Copy .env.example to .env and fill in real API keys."
            )
        }
    }

    private fun requireNonPlaceholder(key: String): String {
        val value = env.getProperty(key)
        if (value.isNullOrBlank()) {
            fail(
                "Missing required key '$key' in .env file.\n" +
                "Copy .env.example to .env and fill in real values."
            )
        }
        if (PLACEHOLDER_INDICATORS.any { value.contains(it, ignoreCase = true) }) {
            fail(
                "Key '$key' in .env still has a placeholder value: '$value'.\n" +
                "Replace it with a real value before running tests."
            )
        }
        return value
    }
}
