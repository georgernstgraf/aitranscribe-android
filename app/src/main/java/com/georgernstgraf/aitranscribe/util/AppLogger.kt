package com.georgernstgraf.aitranscribe.util

import android.util.Log
import com.georgernstgraf.aitranscribe.domain.usecase.TranscribeAudioUseCase
import com.georgernstgraf.aitranscribe.domain.usecase.TranscriptionException
import com.georgernstgraf.aitranscribe.domain.usecase.TranscribeAudioUseCase.TranscriptionException
import kotlinx.coroutines.CoroutineExceptionHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized logging utility.
 * Provides structured logging with different levels.
 */
@Singleton
class AppLogger @Inject constructor() {

    private val TAG = "AITranscribe"

    /**
     * Log debug message.
     */
    fun d(message: String) {
        Log.d(TAG, message)
    }

    /**
     * Log debug message with throwable.
     */
    fun d(message: String, throwable: Throwable) {
        Log.d(TAG, message, throwable)
    }

    /**
     * Log info message.
     */
    fun i(message: String) {
        Log.i(TAG, message)
    }

    /**
     * Log info message with throwable.
     */
    fun i(message: String, throwable: Throwable) {
        Log.i(TAG, message, throwable)
    }

    /**
     * Log warning message.
     */
    fun w(message: String) {
        Log.w(TAG, message)
    }

    /**
     * Log warning message with throwable.
     */
    fun w(message: String, throwable: Throwable) {
        Log.w(TAG, message, throwable)
    }

    /**
     * Log error message.
     */
    fun e(message: String) {
        Log.e(TAG, message)
    }

    /**
     * Log error message with throwable.
     */
    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    /**
     * Log transcription operation.
     */
    fun logTranscription(action: String, audioPath: String, duration: Long? = null) {
        i("Transcription: $action, path=$audioPath, duration=${duration ?: "N/A"}s")
    }

    /**
     * Log API error.
     */
    fun logApiError(action: String, exception: TranscriptionException) {
        e("API Error: $action, code=${exception.errorCode}, message=${exception.message}", exception)
    }

    /**
     * Log network status.
     */
    fun logNetworkStatus(isConnected: Boolean) {
        i("Network status: ${if (isConnected) "CONNECTED" else "DISCONNECTED"}")
    }

    /**
     * Log worker operation.
     */
    fun logWorkerOperation(workerClass: String, operation: String, success: Boolean) {
        val status = if (success) "SUCCESS" else "FAILED"
        i("Worker: $workerClass, operation=$operation, status=$status")
    }

    /**
     * Create coroutine exception handler with logging.
     */
    fun createExceptionHandler(): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { context, throwable ->
            e("Coroutine exception in ${context.javaClass.simpleName}", throwable)
        }
    }
}