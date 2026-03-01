package com.georgernstgraf.aitranscribe.data.repository

import com.georgernstgraf.aitranscribe.data.local.AppPreferencesDao
import com.georgernstgraf.aitranscribe.data.local.AppPreferenceEntity
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Repository for app preferences.
 */
@ViewModelScoped
class AppPreferencesRepository @Inject constructor(
    private val appPreferencesDao: AppPreferencesDao
) {

    suspend fun setPreference(key: String, value: String) {
        val entity = AppPreferenceEntity(
            key = key,
            value = value,
            updatedAt = java.time.LocalDateTime.now().toString()
        )
        appPreferencesDao.insert(entity)
    }

    fun getPreferenceFlow(key: String): Flow<String?> {
        return appPreferencesDao.getByKeyFlow(key).map { it?.value }
    }

    suspend fun getPreference(key: String): String? {
        return appPreferencesDao.getByKey(key)?.value
    }

    suspend fun removePreference(key: String) {
        appPreferencesDao.deleteByKey(key)
    }

    suspend fun getAllPreferences(): Map<String, String> {
        return appPreferencesDao.getAll()
            .map { it.associate { it.key to it.value } }
    }

    suspend fun exportPreferences(): List<AppPreferenceEntity> {
        return appPreferencesDao.getAll()
    }

    suspend fun importPreferences(preferences: List<AppPreferenceEntity>) {
        preferences.forEach { preference ->
            appPreferencesDao.insert(preference)
        }
    }
}