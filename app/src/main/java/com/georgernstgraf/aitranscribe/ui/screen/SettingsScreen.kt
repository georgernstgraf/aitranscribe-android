package com.georgernstgraf.aitranscribe.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.georgernstgraf.aitranscribe.domain.model.ProviderConfig
import com.georgernstgraf.aitranscribe.ui.components.DeleteOldDialog
import com.georgernstgraf.aitranscribe.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            snackbarHostState.showSnackbar(
                message = "Settings saved",
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "OK",
                duration = SnackbarDuration.Long
            )
        }
    }

    LaunchedEffect(state.deletedCount) {
        state.deletedCount?.let { count ->
            snackbarHostState.showSnackbar(
                message = "Deleted $count transcriptions",
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(showDeleteDialog) {
        if (showDeleteDialog) {
            viewModel.getOldCount(state.daysToDelete)
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "API Keys",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.groqApiKey ?: "",
                onValueChange = { viewModel.onGroqApiKeyChanged(if (it.isBlank()) null else it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                label = { Text("GROQ API Key") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.openRouterApiKey ?: "",
                onValueChange = { viewModel.onOpenRouterApiKeyChanged(if (it.isBlank()) null else it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                label = { Text("OpenRouter API Key") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.zaiApiKey ?: "",
                onValueChange = { viewModel.onZaiApiKeyChanged(if (it.isBlank()) null else it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                label = { Text("ZAI API Key") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Speech-to-Text",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SttProviderDropdown(
                selectedProvider = state.sttProvider,
                onProviderSelected = { viewModel.onSttProviderChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            SttModelDropdown(
                selectedProvider = state.sttProvider,
                selectedModel = state.sttModel,
                onModelSelected = { viewModel.onSttModelChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "LLM Post-Processing",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LlmProviderDropdown(
                selectedProvider = state.llmProvider,
                onProviderSelected = { viewModel.onLlmProviderChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LlmModelDropdown(
                selectedProvider = state.llmProvider,
                selectedModel = state.llmModel,
                onModelSelected = { viewModel.onLlmModelChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val canSave = !state.groqApiKey.isNullOrBlank() &&
                !state.isValidating &&
                when (state.llmProvider) {
                    "openrouter" -> !state.openRouterApiKey.isNullOrBlank()
                    "zai" -> !state.zaiApiKey.isNullOrBlank()
                    else -> false
                }

            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                enabled = canSave
            ) {
                if (state.isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text("Save Settings")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                Text("Delete Old Transcriptions")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sharing",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.resetPreferredShareApp() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
            ) {
                Text("Reset Preferred Share App")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDeleteDialog) {
        DeleteOldDialog(
            onDismiss = { showDeleteDialog = false },
            onDelete = { daysOld, viewFilter ->
                viewModel.deleteOldTranscriptions(daysOld, viewFilter)
                showDeleteDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SttProviderDropdown(
    selectedProvider: String,
    onProviderSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val providers = ProviderConfig.sttProviders

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = ProviderConfig.getSttProviderDisplayName(selectedProvider),
            onValueChange = {},
            readOnly = true,
            label = { Text("STT Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            providers.forEach { provider ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(provider.displayName) },
                    onClick = {
                        onProviderSelected(provider.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SttModelDropdown(
    selectedProvider: String,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val models = ProviderConfig.getSttModelsForProvider(selectedProvider)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedModel,
            onValueChange = {},
            readOnly = true,
            label = { Text("STT Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            models.forEach { model ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmProviderDropdown(
    selectedProvider: String,
    onProviderSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val providers = ProviderConfig.llmProviders

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = ProviderConfig.getLlmProviderDisplayName(selectedProvider),
            onValueChange = {},
            readOnly = true,
            label = { Text("LLM Provider") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            providers.forEach { provider ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(provider.displayName) },
                    onClick = {
                        onProviderSelected(provider.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmModelDropdown(
    selectedProvider: String,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val models = ProviderConfig.getLlmModelsForProvider(selectedProvider)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedModel,
            onValueChange = {},
            readOnly = true,
            label = { Text("LLM Model") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            models.forEach { model ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelSelected(model)
                        expanded = false
                    }
                )
            }
        }
    }
}
