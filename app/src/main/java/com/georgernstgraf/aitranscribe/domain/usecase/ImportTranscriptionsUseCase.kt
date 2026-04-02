package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Use case for importing transcriptions from file.
 * Supports JSON and CSV formats.
 */
@ViewModelScoped
class ImportTranscriptionsUseCase @Inject constructor(
    private val repository: TranscriptionRepository
) {

    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    suspend operator fun invoke(filePath: String): Int = withContext(Dispatchers.IO) {
        val file = File(filePath)
        
        if (!file.exists()) {
            throw ImportException("File does not exist: $filePath")
        }

        val extension = file.extension.lowercase()
        
        when (extension) {
            "json" -> importFromJson(file)
            "csv" -> importFromCsv(file)
            else -> throw ImportException("Unsupported file format: $extension")
        }
    }

    private suspend fun importFromJson(file: File): Int {
        val json = file.readText()
        
        try {
            val transcriptionsType = object : TypeToken<List<TranscriptionEntity>>() {}.type
            val transcriptions: List<TranscriptionEntity> = gson.fromJson(json, transcriptionsType)

            if (transcriptions.isEmpty()) {
                return 0
            }

            var importedCount = 0
            transcriptions.forEach { transcription ->
                try {
                    repository.insert(transcription)
                    importedCount++
                } catch (e: Exception) {
                    // Skip failed imports
                }
            }
            
            return importedCount
        } catch (e: Exception) {
            throw ImportException("Invalid JSON format: ${e.message}")
        }
    }

    private suspend fun importFromCsv(file: File): Int {
        val lines = file.readLines()

        if (lines.isEmpty()) {
            return 0
        }

        var importedCount = 0
        lines.drop(1).forEach { line ->
            try {
                val fields = parseCsvLine(line)
                if (fields.size >= 3) {
                    val textField = fields[1]
                    val audioPathField: String? = null
                    val createdAtField = fields[2]
                    val entity = TranscriptionEntity(
                        id = fields[0].toLongOrNull() ?: 0,
                        sttText = unescapeCsv(textField),
                        cleanedText = null,
                        audioFilePath = audioPathField,
                        createdAt = createdAtField,
                        errorMessage = null
                    )

                    repository.insert(entity)
                    importedCount++
                }
            } catch (e: Exception) {
                // Skip failed imports
            }
        }

        return importedCount
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        line.forEach { char ->
            when {
                char == '"' && !inQuotes -> {
                    inQuotes = true
                    current.append(char)
                }
                char == '"' && inQuotes -> {
                    inQuotes = false
                    current.append(char)
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        fields.add(current.toString())

        return fields.map { it.trim() }
    }

    private fun unescapeCsv(value: String): String {
        return if (value.startsWith("\"") && value.endsWith("\"")) {
            value.substring(1, value.length - 1)
                .replace("\"\"", "\"")
        } else {
            value
        }
    }

    class ImportException(message: String) : Exception(message)
}
