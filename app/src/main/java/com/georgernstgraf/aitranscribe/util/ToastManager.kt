package com.georgernstgraf.aitranscribe.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ToastMessage(val message: String, val isError: Boolean = false)

@Singleton
class ToastManager @Inject constructor() {
    private val _messages = MutableSharedFlow<ToastMessage>()
    val messages = _messages.asSharedFlow()

    suspend fun showToast(message: String, isError: Boolean = false) {
        _messages.emit(ToastMessage(message, isError))
    }
}
