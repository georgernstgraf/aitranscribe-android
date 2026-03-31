package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

/**
 * Entity for storing app preferences in database.
 * Alternative to DataStore for simpler backup/restore.
 */
@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey
    val key: String,

    val value: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
