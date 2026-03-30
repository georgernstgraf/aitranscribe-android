package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "models",
    primaryKeys = ["id", "provider_id"],
    foreignKeys = [
        ForeignKey(
            entity = ProviderEntity::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["provider_id"])
    ]
)
data class ModelEntity(
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "provider_id")
    val providerId: String,
    
    @ColumnInfo(name = "model_name")
    val modelName: String,
    
    @ColumnInfo(name = "capabilities")
    val capabilities: String? = null // Future-proofing: store JSON list of capabilities (e.g. ["stt", "llm", "vision"])
)
