package com.georgernstgraf.aitranscribe.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.ui.viewmodel.TranscriptionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionDetailScreen(
    transcriptionId: Long,
    viewFilter: ViewFilter,
    navController: NavController,
    viewModel: TranscriptionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.isCopiedToClipboard) {
        if (state.isCopiedToClipboard) {
            snackbarHostState.showSnackbar(
                message = "Copied to clipboard",
                actionLabel = "OK",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            val nextId = state.nextTranscriptionId
            if (nextId != null) {
                navController.navigate("transcription/$nextId/${viewFilter.name}") {
                    popUpTo("transcription/$transcriptionId/${viewFilter.name}") { inclusive = true }
                }
            } else {
                navController.navigateUp()
            }
        }
    }

    LaunchedEffect(state.navigateToId) {
        val targetId = state.navigateToId
        if (targetId != null) {
            viewModel.clearNavigation()
            navController.navigate("transcription/$targetId/${viewFilter.name}") {
                popUpTo("transcription/$transcriptionId/${viewFilter.name}") { inclusive = true }
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Transcription") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateToPrev() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                    }
                    IconButton(onClick = { viewModel.navigateToNext() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                    }
                    IconButton(onClick = { viewModel.copyToClipboard() }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy to clipboard")
                    }
                    IconButton(onClick = { viewModel.toggleViewStatus(transcriptionId) }) {
                        Icon(
                            if (state.isViewed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.isViewed) "Mark as unread" else "Mark as read"
                        )
                    }
                    IconButton(onClick = { viewModel.deleteTranscription(transcriptionId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        val bgColor = if (state.transcription?.isUnviewed == true) Color(0xFF1E3044) else Color(0xFF172028)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            state.transcription?.let { transcription ->
                var editText by remember(transcription.id) {
                    mutableStateOf(transcription.originalText)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        transcription.summary?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { viewModel.toggleViewStatus(transcriptionId) }
                    ) {
                        Text(if (state.isViewed) "Mark Unread" else "Mark Read")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.updateText(transcription.id, editText) },
                        enabled = editText != transcription.originalText
                    ) {
                        Text("Save")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}