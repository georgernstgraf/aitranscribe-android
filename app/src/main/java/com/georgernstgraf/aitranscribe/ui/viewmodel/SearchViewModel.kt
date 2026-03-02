package com.georgernstgraf.aitranscribe.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.SearchTranscriptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchTranscriptionsUseCase: SearchTranscriptionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onStartDateChanged(date: String?) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun onEndDateChanged(date: String?) {
        _uiState.update { it.copy(endDate = date) }
    }

    fun onSearchQueryChanged(query: String?) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onViewFilterChanged(filter: ViewFilter) {
        _uiState.update { it.copy(viewFilter = filter) }
    }

    fun performSearch() {
        viewModelScope.launch {
            try {
                searchTranscriptionsUseCase(
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate,
                    searchQuery = _uiState.value.searchQuery,
                    viewFilter = _uiState.value.viewFilter
                ).collect { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(searchResults = emptyList()) }
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            SearchUiState(
                startDate = null,
                endDate = null,
                searchQuery = null,
                viewFilter = ViewFilter.UNVIEWED_ONLY
            )
        }
    }
}

data class SearchUiState(
    val startDate: String? = null,
    val endDate: String? = null,
    val searchQuery: String? = null,
    val viewFilter: ViewFilter = ViewFilter.UNVIEWED_ONLY,
    val searchResults: List<Transcription> = emptyList()
)