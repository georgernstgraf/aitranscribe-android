package com.georgernstgraf.aitranscribe.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.georgernstgraf.aitranscribe.data.local.ModelEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableModelDropdown(
    selectedModel: String,
    models: List<ModelEntity>,
    onModelSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(selectedModel) }

    // When the selected model changes externally (e.g. initial load), update the search query
    LaunchedEffect(selectedModel) {
        if (!expanded) {
            searchQuery = selectedModel
        }
    }

    val filteredModels = remember(searchQuery, models, expanded) {
        if (!expanded) return@remember models
        
        val terms = searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (terms.isEmpty()) {
            models
        } else {
            models.filter { model ->
                val modelNameLower = model.modelName.lowercase()
                terms.all { term -> modelNameLower.contains(term) }
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                expanded = true
            },
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                searchQuery = selectedModel // Revert search query to actual selected model on dismiss
            }
        ) {
            if (filteredModels.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No models found") },
                    onClick = { },
                    enabled = false
                )
            } else {
                filteredModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model.modelName) },
                        onClick = {
                            onModelSelected(model.externalId)
                            searchQuery = model.externalId
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
