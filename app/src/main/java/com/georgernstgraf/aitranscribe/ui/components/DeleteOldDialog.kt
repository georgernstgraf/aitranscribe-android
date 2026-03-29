package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter

@Composable
fun DeleteOldDialog(
    onDismiss: () -> Unit,
    onDelete: (daysOld: Int, viewFilter: ViewFilter) -> Unit
) {
    var daysOld by remember { mutableStateOf(30) }
    var viewFilter by remember { mutableStateOf(ViewFilter.UNVIEWED_ONLY) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete Everything?") },
            text = { Text("This will delete ALL matching transcriptions regardless of age. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(0, viewFilter)
                        confirmDeleteAll = false
                        onDismiss()
                    }
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Old Transcriptions") },
        text = {
            Column {
                Text("Transcriptions older than:")

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = daysOld.toFloat(),
                    onValueChange = { daysOld = it.toInt() },
                    valueRange = 0f..365f,
                    steps = 11,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (daysOld == 0) "All time" else "$daysOld days",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                if (daysOld == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will delete ALL matching transcriptions!",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Delete:")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = viewFilter == ViewFilter.VIEWED,
                        onClick = { viewFilter = ViewFilter.VIEWED }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Read")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = viewFilter == ViewFilter.UNVIEWED_ONLY,
                        onClick = { viewFilter = ViewFilter.UNVIEWED_ONLY }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unread")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = viewFilter == ViewFilter.ALL,
                        onClick = { viewFilter = ViewFilter.ALL }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("All")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (daysOld == 0) {
                        confirmDeleteAll = true
                    } else {
                        onDelete(daysOld, viewFilter)
                        onDismiss()
                    }
                }
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}