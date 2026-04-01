package com.georgernstgraf.aitranscribe.ui.viewmodel

import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import com.georgernstgraf.aitranscribe.domain.usecase.SearchTranscriptionsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private lateinit var repository: FakeTranscriptionRepository
    private lateinit var useCase: SearchTranscriptionsUseCase
    private lateinit var viewModel: SearchViewModel

    @BeforeEach
    fun setup() {
        repository = FakeTranscriptionRepository()
        useCase = SearchTranscriptionsUseCase(repository)
        viewModel = SearchViewModel(useCase)
    }

    @Test
    fun `initial state has empty search results`() = runTest {
        val state = viewModel.uiState.first()
        assertEquals(0, state.searchResults.size)
        assertNull(state.startDate)
        assertNull(state.endDate)
        assertNull(state.searchQuery)
        assertEquals(ViewFilter.UNVIEWED_ONLY, state.viewFilter)
    }

    @Test
    fun `onSearchQueryChanged updates state`() = runTest {
        viewModel.onSearchQueryChanged("test query")

        val state = viewModel.uiState.first()
        assertEquals("test query", state.searchQuery)
    }

    @Test
    fun `onViewFilterChanged updates state`() = runTest {
        viewModel.onViewFilterChanged(ViewFilter.ALL)

        val state = viewModel.uiState.first()
        assertEquals(ViewFilter.ALL, state.viewFilter)
    }

    @Test
    fun `onStartDateChanged updates state`() = runTest {
        viewModel.onStartDateChanged("2026-01-01")

        val state = viewModel.uiState.first()
        assertEquals("2026-01-01", state.startDate)
    }

    @Test
    fun `onEndDateChanged updates state`() = runTest {
        viewModel.onEndDateChanged("2026-12-31")

        val state = viewModel.uiState.first()
        assertEquals("2026-12-31", state.endDate)
    }

    @Test
    fun `clearSearch resets state`() = runTest {
        viewModel.onSearchQueryChanged("test")
        viewModel.onStartDateChanged("2026-01-01")
        viewModel.onViewFilterChanged(ViewFilter.ALL)

        viewModel.clearSearch()

        val state = viewModel.uiState.first()
        assertNull(state.searchQuery)
        assertNull(state.startDate)
        assertEquals(ViewFilter.UNVIEWED_ONLY, state.viewFilter)
    }
}
