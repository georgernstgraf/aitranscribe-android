package com.georgernstgraf.aitranscribe.ui.screen.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.georgernstgraf.aitranscribe.ui.viewmodel.ProviderAuthResult
import com.georgernstgraf.aitranscribe.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderAuthScreen(
    providerId: String,
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var authToken by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(providerId) {
        val existingToken = viewModel.getProviderToken(providerId)
        if (!existingToken.isNullOrBlank()) {
            authToken = existingToken
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authenticate ${providerId.replaceFirstChar { it.uppercase() }}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text("Enter your authentication credentials for $providerId.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = authToken,
                    onValueChange = {
                        authToken = it
                        validationError = null
                    },
                    label = { Text("Token / API Key") },
                    isError = validationError != null,
                    supportingText = validationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        validationError = null
                        isValidating = true
                        scope.launch {
                            when (val result = viewModel.validateAndSaveProviderAuth(providerId, authToken)) {
                                is ProviderAuthResult.Success -> {
                                    isValidating = false
                                    navController.popBackStack()
                                }
                                is ProviderAuthResult.Error -> {
                                    isValidating = false
                                    validationError = result.message
                                }
                            }
                        }
                    },
                    enabled = !isValidating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying...")
                    } else {
                        Text("Save Credentials")
                    }
                }
            }
        }
    }
}
