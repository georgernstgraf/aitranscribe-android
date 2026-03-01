package com.georgernstgraf.aitranscribe.domain.usecase

import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case for exporting transcriptions to file.
 * Supports JSON and CSV formats with filtering.
 */
@ViewModelScoped
class ExportTranscriptionsUseCase @Inject constructor(
    private val repository: TranscriptionRepository,
    private val outputDirectory: String
) {

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        .create()

    suspend operator fun invoke(
        format: String = "json",
        startDate: String? = null,
        endDate: String? = null,
        viewFilter: ViewFilter = ViewFilter.ALL
    ): String = withContext(Dispatchers.IO) {
        val transcriptions = repository.searchTranscriptions(
            startDate = startDate,
            endDate = endDate,
            searchQuery = null,
            viewFilter = viewFilter
        ).first()

        if (transcriptions.isEmpty()) {
            throw ExportException("No transcriptions to export")
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val extension = if (format.lowercase() == "json") "json" else "csv"
        val fileName = "aitranscribe_export_$timestamp.$extension"
        val outputFile = File(outputDirectory, fileName)

        when (format.lowercase()) {
            "json" -> exportToJson(transcriptions, outputFile)
            "csv" -> exportToCsv(transcriptions, outputFile)
            else -> throw ExportException("Unsupported format: $format")
        }

        outputFile.absolutePath
    }

    private fun exportToJson(transcriptions: List<Transcription>, outputFile: File) {
        val json = gson.toJson(transcriptions)
        outputFile.writeText(json)
    }

    private fun exportToCsv(transcriptions: List<Transcription>, outputFile: File) {
        val csv = buildString {
            appendLine("ID,Original Text,Processed Text,Created At,Status,Played Count")
            transcriptions.forEach { transcription ->
                val originalEscaped = escapeCsv(transcription.originalText)
                val processedEscaped = escapeCsv(transcription.processedText ?: "")
                val createdAtEscaped = escapeCsv(transcription.createdAt.toString())
                
                appendLine("${transcription.id},$originalEscaped,$processedEscaped,$createdAtEscaped,${transcription.status},${transcription.playedCount}")
            }
        }
        outputFile.writeText(csv)
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

    class ExportException(message: String) : Exception(message)
}