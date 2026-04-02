package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LanguageDao {
    @Query("SELECT * FROM languages WHERE is_active = 1 ORDER BY name")
    fun getActiveLanguages(): Flow<List<LanguageEntity>>

    @Query("SELECT * FROM languages WHERE id = :id")
    suspend fun getLanguageById(id: String): LanguageEntity?

    @Query("SELECT name FROM languages WHERE id = :id")
    suspend fun getLanguageName(id: String): String?

    @Query("SELECT EXISTS(SELECT 1 FROM languages WHERE id = :id)")
    suspend fun languageExists(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguage(language: LanguageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLanguages(languages: List<LanguageEntity>)
}
