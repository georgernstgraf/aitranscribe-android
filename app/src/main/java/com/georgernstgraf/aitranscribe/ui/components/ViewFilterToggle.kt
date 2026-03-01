package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter

@Composable
fun ViewFilterToggle(
    currentFilter: ViewFilter,
    onFilterChanged: (ViewFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = currentFilter == ViewFilter.ALL,
            onClick = { onFilterChanged(ViewFilter.ALL) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        FilterChip(
            selected = currentFilter == ViewFilter.UNVIEWED_ONLY,
            onClick = { onFilterChanged(ViewFilter.UNVIEWED_ONLY) },
            label = { Text("Only Unviewed") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
    }
}