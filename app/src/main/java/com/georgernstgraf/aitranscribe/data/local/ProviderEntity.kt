package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = 0L,

    @ColumnInfo(name = "api_token")
    val apiToken: String? = null
)
