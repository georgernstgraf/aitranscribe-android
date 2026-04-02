package com.georgernstgraf.aitranscribe.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "languages")
data class LanguageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "native_name")
    val nativeName: String?,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
