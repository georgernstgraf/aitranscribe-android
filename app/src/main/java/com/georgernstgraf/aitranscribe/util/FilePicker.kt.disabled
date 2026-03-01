package com.georgernstgraf.aitranscribe.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.georgernstgraf.aitranscribe.AITranscribeApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * File picker utility for selecting export files.
 */
@Singleton
class FilePicker @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Create file picker contract for import.
     */
    fun createImportFilePicker(): ActivityResultContracts.GetContent {
        return ActivityResultContracts.GetContent().apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            addCategory(android.content.Intent.CATEGORY_OPENABLE_DOCUMENT)
            type = "*/*"
            val mimeTypes = arrayOf(
                "application/json",
                "text/csv",
                "text/plain"
            )
            putExtra(android.content.Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
    }

    /**
     * Create directory picker contract for export.
     */
    fun createExportDirectoryPicker(): ActivityResultContracts.CreateDocument {
        return ActivityResultContracts.CreateDocument("text/plain")
    }

    /**
     * Save file to external storage.
     */
    suspend fun saveFile(fileName: String, content: String): Uri? = withContext(Dispatchers.IO) {
        return try {
            val pickerContract = createExportDirectoryPicker()
            pickerContract.launch(fileName)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Read file content.
     */
    suspend fun readFile(uri: Uri): String? = withContext(Dispatchers.IO) {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get file from URI.
     */
    suspend fun getFile(uri: Uri): File? = withContext(Dispatchers.IO) {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}