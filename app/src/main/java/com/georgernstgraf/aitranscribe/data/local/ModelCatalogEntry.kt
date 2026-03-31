package com.georgernstgraf.aitranscribe.data.local

data class ModelCatalogEntry(
    val externalId: String,
    val modelName: String,
    val capabilities: List<CapabilityEntity>
)
