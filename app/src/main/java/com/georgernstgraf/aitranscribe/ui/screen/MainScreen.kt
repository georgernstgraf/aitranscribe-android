package com.georgernstgraf.aitranscribe.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.georgernstgraf.aitranscribe.ui.components.QuickFilters
import com.georgernstgraf.aitranscribe.ui.components.StatisticsCard
import com.georgernstgraf.aitranscribe.ui.components.TranscriptionItem
import com.georgernstgraf.aitranscribe.ui.components.TranscriptionActions
import com.georgernstgraf.aitranscribe.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExportDialog by remember { mutableStateOf(false) }

    val totalCount = state.recentTranscriptions.size
    val unviewedCount = state.recentTranscriptions.count { it.isUnviewed }
    val processedCount = state.recentTranscriptions.count { it.processedText != null }

    LaunchedEffect(state.recordingError) {
        state.recordingError?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "OK",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AITranscribe") },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("search") }
                    ) {
                        androidx.compose.material.icons.Icons.Default.Search.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = "Search"
                            )
                        }
                    }

                    IconButton(
                        onClick = { navController.navigate("settings") }
                    ) {
                        androidx.compose.material.icons.Icons.Default.Settings.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = "Settings"
                            )
                        }
                    }

                    IconButton(
                        onClick = { showExportDialog = true }
                    ) {
                        androidx.compose.material.icons.Icons.Default.Share.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = "Export"
                            )
                        }
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
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            StatisticsCard(
                totalCount = totalCount,
                unviewedCount = unviewedCount,
                processedCount = processedCount,
                averageLength = 150.0,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            QuickFilters(
                currentFilter = com.georgernstgraf.aitranscribe.ui.components.QuickFilters.ALL,
                onFilterChanged = {  },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecordingButton(
                    isRecording = state.isRecording,
                    recordingDuration = state.recordingDuration,
                    onStartRecording = { viewModel.startRecording() },
                    onStopRecording = { viewModel.stopRecording() },
                    modifier = Modifier
                )

                Text(
                    text = if (state.isRecording) {
                        "Recording... ${state.recordingDuration}s"
                    } else {
                        "Push & Hold to Record"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.recentTranscriptions.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material.icons.Icons.Default.Mic.let { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No transcriptions yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Record your first transcription by pushing & holding the button above",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Recent Transcriptions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    items(state.recentTranscriptions) { transcription ->
                        TranscriptionItem(
                            transcription = transcription,
                            onClick = {
                                navController.navigate("transcription/${transcription.id}")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showExportDialog) {
        com.georgernstgraf.aitranscribe.ui.components.ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                viewModel.onExportRequested(format)
            }
        )
    }
}