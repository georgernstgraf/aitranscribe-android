package com.georgernstgraf.aitranscribe.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.georgernstgraf.aitranscribe.ui.viewmodel.SetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isSetupComplete) {
        if (state.isSetupComplete) {
            onSetupComplete()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "OK",
                duration = SnackbarDuration.Short
            )
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Setup AITranscribe") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to AITranscribe",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Enter your API keys to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = state.groqApiKey ?: "",
                onValueChange = { viewModel.onGroqApiKeyChanged(if (it.isBlank()) null else it) },
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text("GROQ API Key") },
                placeholder = { Text("gsk_...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                trailingIcon = {
                    when {
                        state.groqApiKey.isNullOrBlank() -> Unit
                        state.isGroqKeyValid == true -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Valid",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        state.isGroqKeyValid == false -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Invalid",
                            tint = MaterialTheme.colorScheme.error
                        )
                        else -> Unit
                    }
                },
                isError = state.groqKeyError != null,
                supportingText = {
                    state.groqKeyError?.let { error ->
                        Text(
                            text = when (error) {
                                com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError.MISSING -> "API key is required"
                                com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError.INVALID_FORMAT -> "Invalid GROQ API key format"
                                com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError.API_ERROR -> "Failed to validate API key"
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.openRouterApiKey ?: "",
                onValueChange = { viewModel.onOpenRouterApiKeyChanged(if (it.isBlank()) null else it) },
                modifier = Modifier
                    .fillMaxWidth(),
                label = { Text("OpenRouter API Key") },
                placeholder = { Text("sk-or-...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                trailingIcon = {
                    when {
                        state.openRouterApiKey.isNullOrBlank() -> Unit
                        state.isOpenRouterKeyValid == true -> Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Valid",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        state.isOpenRouterKeyValid == false -> Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Invalid",
                            tint = MaterialTheme.colorScheme.error
                        )
                        else -> Unit
                    }
                },
                isError = state.openRouterKeyError != null,
                supportingText = {
                    state.openRouterKeyError?.let { error ->
                        Text(
                            text = when (error) {
                                com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError.MISSING -> "API key is required"
                                com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError.INVALID_FORMAT -> "Invalid OpenRouter API key format"
                                com.georgernstgraf.aitranscribe.domain.usecase.ApiKeyError.API_ERROR -> "Failed to validate API key"
                            },
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.isValidating) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Validating API keys...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(
                        onClick = { viewModel.validateAndSave() }
                    ) {
                        Text("Retry")
                    }
                }
            }

            if (state.isValidating) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.validateAndSave() },
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = !state.groqApiKey.isNullOrBlank() && 
                              !state.openRouterApiKey.isNullOrBlank() &&
                              !state.isValidating
                ) {
                    Text("Get Started")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your API keys are stored securely on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Get your GROQ API key from console.groq.com",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Get your OpenRouter API key from openrouter.ai/keys",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
