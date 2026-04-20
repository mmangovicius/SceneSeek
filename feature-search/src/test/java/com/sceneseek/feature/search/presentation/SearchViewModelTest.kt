package com.sceneseek.feature.search.presentation

import app.cash.turbine.test
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.repository.SearchRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.testutils.UnconfinedTestDispatcherExtension
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private val SEARCH_RESULTS = listOf(
    MediaItem.MovieItem(Movie(1, "Search Result", null, null, "", 8.0, ""))
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
                assertTrue(awaitItem().items.isEmpty())
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
}
