package com.georgernstgraf.aitranscribe.ui.viewmodel

import app.cash.turbine.test
import com.georgernstgraf.aitranscribe.data.testing.FakeTranscriptionRepository
import com.georgernstgraf.aitranscribe.domain.model.Transcription
import com.georgernstgraf.aitranscribe.domain.model.ViewFilter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

/**
 * Test class for SearchViewModel.
 * Tests search screen UI logic.
 */
class SearchViewModelTest {

    private lateinit var viewModel: SearchViewModel
    private lateinit var fakeRepository: FakeTranscriptionRepository

    @Before
    fun setup() {
        fakeRepository = FakeTranscriptionRepository()
        viewModel = SearchViewModel(fakeRepository)
    }

    @Test
    fun `initial state has no filters`() = runTest {
        val initialState = viewModel.uiState.value

        assertEquals("Start date should be null", null, initialState.startDate)
        assertEquals("End date should be null", null, initialState.endDate)
        assertEquals("Search query should be null", null, initialState.searchQuery)
        assertEquals("View filter should be UNVIEWED_ONLY", ViewFilter.UNVIEWED_ONLY, initialState.viewFilter)
        assertTrue("Results should be empty initially", initialState.searchResults.isEmpty())
    }

    @Test
    fun `updateStartDate updates state`() = runTest {
        val testDate = "2024-01-01"

        viewModel.onStartDateChanged(testDate)

        assertEquals("Start date should be updated", testDate, viewModel.uiState.value.startDate)
    }

    @Test
    fun `updateEndDate updates state`() = runTest {
        val testDate = "2024-12-31"

        viewModel.onEndDateChanged(testDate)

        assertEquals("End date should be updated", testDate, viewModel.uiState.value.endDate)
    }

    @Test
    fun `updateSearchQuery updates state`() = runTest {
        val query = "meeting"

        viewModel.onSearchQueryChanged(query)

        assertEquals("Search query should be updated", query, viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `updateViewFilter updates state`() = runTest {
        viewModel.onViewFilterChanged(ViewFilter.ALL)

        assertEquals("View filter should be updated", ViewFilter.ALL, viewModel.uiState.value.viewFilter)
    }

    @Test
    fun `performSearch emits search results`() = runTest {
        val transcription1 = createTestTranscription(id = 1, playedCount = 0)
        val transcription2 = createTestTranscription(id = 2, playedCount = 0)
        createTestTranscription(id = 3, playedCount = 1)

        viewModel.uiState.test {
            awaitItem()
            
            viewModel.onSearchQueryChanged("transcription")
            viewModel.performSearch()
            
            skipItems(1)
            
            val state = awaitItem()
            assertEquals("Should return search results", 2, state.searchResults.size)
        }
    }

    @Test
    fun `performSearch filters by view filter`() = runTest {
        createTestTranscription(id = 1, playedCount = 0)
        createTestTranscription(id = 2, playedCount = 0)
        createTestTranscription(id = 3, playedCount = 1)

        viewModel.onViewFilterChanged(ViewFilter.UNVIEWED_ONLY)
        viewModel.performSearch()

        viewModel.uiState.test {
            awaitItem()
            
            skipItems(1)
            
            val state = awaitItem()
            assertEquals("Should return only unviewed", 2, state.searchResults.size)
            assertTrue("All results should be unviewed", state.searchResults.all { it.playedCount == 0 })
        }
    }

    @Test
    fun `performSearch respects date range`() = runTest {
        val now = LocalDateTime.now()
        createTestTranscription(id = 1, createdAt = now.minusDays(2))
        createTestTranscription(id = 2, createdAt = now.minusDays(1))
        createTestTranscription(id = 3, createdAt = now)

        viewModel.onStartDateChanged(now.minusDays(1).toString())
        viewModel.performSearch()

        viewModel.uiState.test {
            awaitItem()
            
            skipItems(1)
            
            val state = awaitItem()
            assertTrue("Should filter by date range", state.searchResults.size >= 2)
        }
    }

    @Test
    fun `clearSearch resets filters`() = runTest {
        viewModel.onSearchQueryChanged("test")
        viewModel.onViewFilterChanged(ViewFilter.ALL)
        viewModel.onStartDateChanged("2024-01-01")

        viewModel.clearSearch()

        val state = viewModel.uiState.value
        assertEquals("Search query should be cleared", null, state.searchQuery)
        assertEquals("View filter should reset to default", ViewFilter.UNVIEWED_ONLY, state.viewFilter)
        assertEquals("Start date should be cleared", null, state.startDate)
        assertEquals("End date should be cleared", null, state.endDate)
    }

    private fun createTestTranscription(
        id: Long,
        playedCount: Int = 0,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Transcription {
        val entity = com.georgernstgraf.aitranscribe.data.local.TranscriptionEntity(
            id = id,
            originalText = "Test transcription $id",
            processedText = null,
            audioFilePath = "/path/to/audio.mp3",
            createdAt = createdAt.toString(),
            postProcessingType = null,
            status = com.georgernstgraf.aitranscribe.domain.model.TranscriptionStatus.COMPLETED.name,
            errorMessage = null,
            playedCount = playedCount,
            retryCount = 0
        )
        fakeRepository.insert(entity)
        return entity.toDomain()
    }
}