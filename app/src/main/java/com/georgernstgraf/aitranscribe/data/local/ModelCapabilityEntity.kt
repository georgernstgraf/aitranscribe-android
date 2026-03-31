package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "model_capabilities",
    primaryKeys = ["model_id", "capability_id"],
    foreignKeys = [
        ForeignKey(
            entity = ModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CapabilityEntity::class,
            parentColumns = ["id"],
            childColumns = ["capability_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["capability_id"])
    ]
)
data class ModelCapabilityEntity(
    @ColumnInfo(name = "model_id")
    val modelId: Long,

    @ColumnInfo(name = "capability_id")
    val capabilityId: String,

    @ColumnInfo(name = "source")
    val source: String? = null
)
