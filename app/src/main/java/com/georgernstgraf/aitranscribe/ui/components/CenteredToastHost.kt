package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.georgernstgraf.aitranscribe.util.ToastManager
import com.georgernstgraf.aitranscribe.util.ToastMessage
import kotlinx.coroutines.delay

@Composable
fun CenteredToastHost(
    toastManager: ToastManager
) {
    var toastState by remember { mutableStateOf<ToastMessage?>(null) }

    LaunchedEffect(toastManager) {
        toastManager.messages.collect { message ->
            toastState = message
            delay(3000) // Auto-hide after 3 seconds
            toastState = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = toastState != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            toastState?.let { toast ->
                Surface(
                    modifier = Modifier
                        .clickable { toastState = null }
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (toast.isError) MaterialTheme.colorScheme.errorContainer 
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = toast.message,
                        modifier = Modifier.padding(16.dp),
                        color = if (toast.isError) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
