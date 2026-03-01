package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.AppPreferencesEntity
import com.georgernstgraf.aitranscribe.data.repository.AppPreferencesRepository
import com.georgernstgraf.aitranscribe.util.FilePicker
import com.google.gson.Gson
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Use case for backup and restore functionality.
 */
@ViewModelScoped
class BackupRestoreUseCase @Inject constructor(
    private val appPreferencesRepository: AppPreferencesRepository,
    private val filePicker: FilePicker
) {

    private val gson = Gson()

    suspend fun createBackup(outputPath: String): File = withContext(Dispatchers.IO) {
        val preferences = appPreferencesRepository.exportPreferences()
        val transcriptionData = gson.toJson(preferences)
        
        val backupFile = File(outputPath)
        backupFile.writeText(transcriptionData)
        backupFile
    }

    suspend fun restoreFromBackup(filePath: String): Int = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw BackupRestoreException("Backup file does not exist")
        }

        val json = file.readText()
        val preferences: List<AppPreferenceEntity> = try {
            val type = object : com.google.gson.reflect.TypeToken<List<AppPreferenceEntity>>() {}
            gson.fromJson(json, type)
        } catch (e: Exception) {
            throw BackupRestoreException("Invalid backup file format")
        }

        appPreferencesRepository.importPreferences(preferences)
        preferences.size
    }

    fun getBackupFileName(): String {
        val timestamp = java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        "aitranscribe_backup_$timestamp.json"
    }

    fun validateBackup(file: File): Boolean {
        return file.exists() && file.canRead()
    }

    fun getBackupSize(file: File): Long {
        return if (file.exists()) file.length() else 0L
    }

    class BackupRestoreException(message: String) : Exception(message)
}