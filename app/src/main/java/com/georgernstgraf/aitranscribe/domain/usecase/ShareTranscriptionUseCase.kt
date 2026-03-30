package com.georgernstgraf.aitranscribe.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.georgernstgraf.aitranscribe.data.local.toDomain
import com.georgernstgraf.aitranscribe.data.repository.TranscriptionRepository
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Use case for sharing transcriptions with other apps.
 * Creates share intent with text content.
 */
@ViewModelScoped
class ShareTranscriptionUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TranscriptionRepository
) {

    suspend operator fun invoke(id: Long): Intent = withContext(Dispatchers.IO) {
        val transcription = repository.getById(id)
            ?: throw ShareException("Transcription not found: $id")

        val text = transcription.toDomain().getShareText()
        
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, transcription.toDomain().getShareTitle())
            
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    suspend fun invokeAsFile(id: Long): Intent = withContext(Dispatchers.IO) {
        val transcription = repository.getById(id)
            ?: throw ShareException("Transcription not found: $id")
        
        val transcriptionDomain = transcription.toDomain()
        val text = transcriptionDomain.getShareText()
        val fileName = "transcription_${transcription.id}.txt"
        
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, createTempFile(fileName, text))
        
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, transcriptionDomain.getShareTitle())
            
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    private fun createTempFile(fileName: String, content: String): File {
        val cacheDir = context.cacheDir
        val file = File(cacheDir, fileName)
        file.writeText(content)
        return file
    }

    class ShareException(message: String) : Exception(message)
}
