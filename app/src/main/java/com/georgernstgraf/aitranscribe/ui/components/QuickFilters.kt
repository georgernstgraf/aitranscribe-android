package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter

@Composable
fun QuickFilters(
    currentFilter: ViewFilter,
    onFilterChanged: (ViewFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ViewFilter.entries.forEach { filter ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text(filter.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

private val ViewFilter.label: String
    get() = when (this) {
        ViewFilter.ALL -> "All"
        ViewFilter.UNVIEWED_ONLY -> "Unviewed"
        ViewFilter.VIEWED -> "Viewed"
    }
