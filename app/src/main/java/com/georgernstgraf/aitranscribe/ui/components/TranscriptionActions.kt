package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Button group for transcription actions.
 */
@Composable
fun TranscriptionActions(
    onViewDetails: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onShare: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onViewDetails,
            modifier = Modifier.weight(1f)
        ) {
            Text("View")
        }

        Button(
            onClick = onCopyToClipboard,
            modifier = Modifier.weight(1f)
        ) {
            Text("Copy")
        }

        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f)
        ) {
            Text("Share")
        }

        OutlinedButton(
            onClick = onMarkAsUnread,
            modifier = Modifier.weight(1f)
        ) {
            Text("Mark Unread")
        }

        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Delete")
        }
    }
}