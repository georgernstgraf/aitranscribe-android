package com.georgernstgraf.aitranscribe.domain.model

data class ProviderSettings(
    val providerId: String,
    val apiKey: String?,
    val selectedModel: String
)
