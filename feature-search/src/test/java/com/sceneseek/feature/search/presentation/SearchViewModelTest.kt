@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.sceneseek.feature.search.presentation

import app.cash.turbine.test
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.SearchRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.testutils.UnconfinedTestDispatcherExtension
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private val SEARCH_RESULTS = listOf(
    MediaItem.MovieItem(Movie(1, "Search Result", null, null, "", 8.0, ""))
)

private val SEARCH_RESULTS_PAGE_2 = listOf(
    MediaItem.MovieItem(Movie(2, "Search Result Page 2", null, null, "", 7.0, ""))
)

@ExtendWith(MockKExtension::class, UnconfinedTestDispatcherExtension::class)
internal class SearchViewModelTest {

    private val searchRepo = mockk<SearchRepository>(relaxed = true)
    private lateinit var viewModel: SearchViewModel

    @BeforeEach
    fun setUp() {
        viewModel = SearchViewModel(searchRepo)
    }

    @Nested
    inner class WhenInitialized {

        @Test
        fun `GIVEN fresh viewmodel WHEN state observed THEN query is empty`() = runTest {
            viewModel.state.test {
                assertTrue(awaitItem().query.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN fresh viewmodel WHEN state observed THEN items are empty`() = runTest {
            viewModel.state.test {
                assertTrue(awaitItem().results.items.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN fresh viewmodel WHEN state observed THEN isLoading is false`() = runTest {
            viewModel.state.test {
                assertFalse(awaitItem().isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class OnQueryChanges {

        @Test
        fun `GIVEN non-empty query WHEN onQueryCleared THEN state resets to empty`() = runTest {
            viewModel.onQueryChanged("some query")
            viewModel.onQueryCleared()

            viewModel.state.test {
                assertTrue(awaitItem().query.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN query longer than 1 char WHEN search returns results THEN state has items`() = runTest {
            every { searchRepo.search("ab", 1) } returns flowOf(Result.Success(SEARCH_RESULTS))

            viewModel.onQueryChanged("ab")

            viewModel.state.test {
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class Pagination {

        @Test
        fun `GIVEN initial search completed WHEN loadMore THEN appends new data`() = runTest {
            every { searchRepo.search("test", 1) } returns flowOf(Result.Success(SEARCH_RESULTS))
            every { searchRepo.search("test", 2) } returns flowOf(Result.Success(SEARCH_RESULTS_PAGE_2))

            viewModel.onQueryChanged("test")
            advanceTimeBy(301)

            viewModel.loadMore()

            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state.results.items.containsAll(SEARCH_RESULTS))
                assertTrue(state.results.items.containsAll(SEARCH_RESULTS_PAGE_2))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN initial search completed WHEN loadMore THEN page increments`() = runTest {
            every { searchRepo.search("test", 1) } returns flowOf(Result.Success(SEARCH_RESULTS))
            every { searchRepo.search("test", 2) } returns flowOf(Result.Success(SEARCH_RESULTS_PAGE_2))

            viewModel.onQueryChanged("test")
            advanceTimeBy(301)

            viewModel.loadMore()

            viewModel.state.test {
                assertEquals(2, awaitItem().results.page)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN empty result from page 2 WHEN loadMore THEN canLoadMore becomes false`() = runTest {
            every { searchRepo.search("test", 1) } returns flowOf(Result.Success(SEARCH_RESULTS))
            every { searchRepo.search("test", 2) } returns flowOf(Result.Success(emptyList()))

            viewModel.onQueryChanged("test")
            advanceTimeBy(301)

            viewModel.loadMore()

            viewModel.state.test {
                assertFalse(awaitItem().results.canLoadMore)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN query too short WHEN loadMore THEN does nothing`() = runTest {
            val initialState = viewModel.state.value

            viewModel.loadMore()

            viewModel.state.test {
                val state = awaitItem()
                assertEquals(initialState.results.items, state.results.items)
                assertEquals(initialState.results.page, state.results.page)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN new query submitted WHEN search completes THEN pagination resets to page 1`() = runTest {
            every { searchRepo.search("test", 1) } returns flowOf(Result.Success(SEARCH_RESULTS))
            every { searchRepo.search("test", 2) } returns flowOf(Result.Success(SEARCH_RESULTS_PAGE_2))
            every { searchRepo.search("other", 1) } returns flowOf(Result.Success(SEARCH_RESULTS))

            viewModel.onQueryChanged("test")
            advanceTimeBy(301)
            viewModel.loadMore()

            viewModel.onQueryChanged("other")
            advanceTimeBy(301)

            viewModel.state.test {
                val state = awaitItem()
                assertEquals(1, state.results.page)
                assertTrue(state.results.canLoadMore)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class OnUserInteraction {

        @Test
        fun `GIVEN movie item WHEN onItemClicked THEN emits NavigateToDetail with movie type`() = runTest {
            viewModel.navEvents.test {
                viewModel.onItemClicked(MediaItem.MovieItem(Movie(1, "Movie", null, null, "", 8.0, "")))
                val event = awaitItem() as SearchNavEvent.NavigateToDetail
                assertEquals(1, event.mediaId)
                assertEquals("movie", event.mediaType)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN tv item WHEN onItemClicked THEN emits NavigateToDetail with tv type`() = runTest {
            viewModel.navEvents.test {
                viewModel.onItemClicked(MediaItem.TvItem(TvShow(10, "Show", null, null, "", 7.5, "")))
                val event = awaitItem() as SearchNavEvent.NavigateToDetail
                assertEquals(10, event.mediaId)
                assertEquals("tv", event.mediaType)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
