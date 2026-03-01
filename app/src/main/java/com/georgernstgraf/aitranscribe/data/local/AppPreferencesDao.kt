package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for app preferences stored in database.
 */
@Dao
interface AppPreferencesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preference: AppPreferenceEntity)

    @Update
    suspend fun update(preference: AppPreferenceEntity)

    @Query("SELECT * FROM app_preferences WHERE key = :key")
    suspend fun getByKey(key: String): AppPreferenceEntity?

    @Query("SELECT * FROM app_preferences WHERE key = :key")
    fun getByKeyFlow(key: String): Flow<AppPreferenceEntity?>

    @Query("SELECT * FROM app_preferences")
    fun getAll(): Flow<List<AppPreferenceEntity>>

    @Query("DELETE FROM app_preferences")
    suspend fun deleteAll()

    @Query("DELETE FROM app_preferences WHERE key = :key")
    suspend fun deleteByKey(key: String)
}