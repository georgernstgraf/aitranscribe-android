package com.georgernstgraf.aitranscribe.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ToastMessage(
    val message: String,
    val isError: Boolean = false,
    val isWarning: Boolean = false,
    val durationMillis: Int = DEFAULT_TOAST_DURATION_MS
)

const val DEFAULT_TOAST_DURATION_MS = 3000

@Singleton
class ToastManager @Inject constructor() {
    private val _messages = MutableSharedFlow<ToastMessage>()
    val messages = _messages.asSharedFlow()

    suspend fun showToast(
        message: String,
        isError: Boolean = false,
        isWarning: Boolean = false,
        durationMillis: Int = DEFAULT_TOAST_DURATION_MS
    ) {
        _messages.emit(ToastMessage(message, isError, isWarning, durationMillis))
    }
}
