package com.georgernstgraf.aitranscribe.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.ui.viewmodel.TranscriptionDetailViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TranscriptionDetailScreen(
    transcriptionId: Long,
    viewFilter: ViewFilter,
    navController: NavController,
    viewModel: TranscriptionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val filteredIds by viewModel.filteredIds.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            navController.navigateUp()
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transcription") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentId = state.transcription?.id ?: transcriptionId
                    state.transcription?.let { trans ->
                        IconButton(onClick = { 
                            val shareIntent = viewModel.shareTranscription(trans)
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                    IconButton(onClick = { viewModel.toggleViewStatus(currentId) }) {
                        Icon(
                            if (state.isViewed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (state.isViewed) "Mark as unread" else "Mark as read"
                        )
                    }
                    IconButton(onClick = { viewModel.deleteTranscription(currentId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->

        if (filteredIds.isEmpty()) {
            return@Scaffold
        }

        val initialIndex = filteredIds.indexOf(transcriptionId).coerceAtLeast(0)
        val pagerState = rememberPagerState(
            initialPage = initialIndex,
            pageCount = { filteredIds.size }
        )

        LaunchedEffect(pagerState.settledPage, filteredIds) {
            viewModel.onPageChanged(pagerState.settledPage)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            val pageId = filteredIds.getOrElse(page) { transcriptionId }
            val isCurrentPage = page == pagerState.settledPage
            val transcription = if (isCurrentPage) state.transcription else null

            val bgColor = if (transcription?.isUnviewed == true) Color(0xFF1E3044) else Color(0xFF172028)
            val focusManager = LocalFocusManager.current
            var isEditing by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(bgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isEditing) {
                            focusManager.clearFocus()
                        }
                    }
                    .verticalScroll(rememberScrollState())
            ) {
                transcription?.let { trans ->
                    var editText by remember(trans.id, trans.text) {
                        mutableStateOf(trans.text ?: "")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            trans.summary?.let { summary ->
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (trans.status == TranscriptionStatus.COMPLETED_WITH_WARNING) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0x33FFA000)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Post-processing incomplete",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFFFA000),
                                            fontWeight = FontWeight.Bold
                                        )
                                        trans.errorMessage?.let { error ->
                                            Text(
                                                text = error,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFCC8000)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cleanup",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = state.isCleanupEnabled,
                                    onCheckedChange = { viewModel.onCleanupToggled(it) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { viewModel.translateToEnglish() },
                                    enabled = !state.isTranslating,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("EN")
                                }
                                TextButton(
                                    onClick = { viewModel.translateToGerman() },
                                    enabled = !state.isTranslating,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("DE")
                                }
                            }

                            OutlinedTextField(
                                value = editText,
                                onValueChange = { editText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusEvent { focusState ->
                                        if (focusState.hasFocus) {
                                            isEditing = true
                                        } else if (isEditing) {
                                            if (editText != (trans.text ?: "")) {
                                                viewModel.updateText(trans.id, editText)
                                            }
                                            isEditing = false
                                        }
                                    },
                                minLines = 3,
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
