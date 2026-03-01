package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog for export transcriptions.
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (format: String) -> Unit
) {
    var selectedFormat by androidx.compose.runtime.mutableStateOf("json")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Transcriptions") },
        text = {
            Column {
                Text("Choose export format:")

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { selectedFormat = "json" },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedFormat == "json") {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text("JSON")
                    }

                    OutlinedButton(
                        onClick = { selectedFormat = "csv" },
                        modifier = Modifier.weight(1f),
                        colors = if (selectedFormat == "csv") {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text("CSV")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onExport(selectedFormat)
                    onDismiss()
                }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}