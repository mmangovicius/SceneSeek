package com.sceneseek.feature.home.presentation

import app.cash.turbine.test
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.util.Result
import com.sceneseek.feature.home.domain.usecase.GetPopularMoviesUseCase
import com.sceneseek.feature.home.domain.usecase.GetPopularTvUseCase
import com.sceneseek.feature.home.domain.usecase.GetTopRatedMoviesUseCase
import com.sceneseek.feature.home.domain.usecase.GetTopRatedTvUseCase
import com.sceneseek.feature.home.domain.usecase.GetTrendingUseCase
import com.sceneseek.testutils.UnconfinedTestDispatcherExtension
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private val MOVIE = Movie(1, "Movie 1", null, null, "", 7.0, "2024-01-01")
private val MOVIE_2 = Movie(2, "Movie 2", null, null, "", 8.0, "2024-06-01")
private val TV_SHOW = TvShow(10, "Show 1", null, null, "", 7.5, "2024-01-01")

@ExtendWith(MockKExtension::class, UnconfinedTestDispatcherExtension::class)
internal class HomeViewModelTest {

    private val getTrending = mockk<GetTrendingUseCase>()
    private val getPopularMovies = mockk<GetPopularMoviesUseCase>()
    private val getPopularTv = mockk<GetPopularTvUseCase>()
    private val getTopRatedMovies = mockk<GetTopRatedMoviesUseCase>()
    private val getTopRatedTv = mockk<GetTopRatedTvUseCase>()

    private fun createViewModel() = HomeViewModel(
        getTrending, getPopularMovies, getPopularTv, getTopRatedMovies, getTopRatedTv
    )

    @BeforeEach
    fun setUp() {
        every { getTrending() } returns flowOf(Result.Success(listOf(MOVIE)))
        every { getPopularMovies(any()) } returns flowOf(Result.Success(listOf(MOVIE_2)))
        every { getPopularTv(any()) } returns flowOf(Result.Success(listOf(TV_SHOW)))
        every { getTopRatedMovies(any()) } returns flowOf(Result.Success(listOf(MOVIE, MOVIE_2)))
        every { getTopRatedTv(any()) } returns flowOf(Result.Success(listOf(TV_SHOW)))
    }

    @Nested
    inner class WhenAllUseCasesSucceed {

        @Test
        fun `GIVEN all use cases succeed WHEN initialized THEN state has data from all 5 sources`() = runTest {
            createViewModel().state.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertFalse(state.hasError)
                assertEquals(1, state.trending.items.size)
                assertEquals(1, state.popularMovies.items.size)
                assertEquals(1, state.popularTv.items.size)
                assertEquals(2, state.topRatedMovies.items.size)
                assertEquals(1, state.topRatedTv.items.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN all use cases succeed WHEN onRetry called THEN content is reloaded`() = runTest {
            val newMovie = Movie(99, "New Movie", null, null, "", 9.0, "2024-12-01")
            every { getTrending() } returns flowOf(Result.Success(listOf(newMovie)))
            val vm = createViewModel()

            vm.onRetry()

            vm.state.test {
                val state = awaitItem()
                assertEquals(newMovie, state.trending.items.first())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class WhenUseCaseFails {

        @Test
        fun `GIVEN trending use case fails WHEN initialized THEN state has error message`() = runTest {
            every { getTrending() } returns flowOf(Result.Error(RuntimeException("Network error")))

            createViewModel().state.test {
                val state = awaitItem()
                assertTrue(state.hasError)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class OnUserInteraction {

        @Test
        fun `GIVEN movie item WHEN onItemClicked THEN emits NavigateToDetail with movie type`() = runTest {
            val vm = createViewModel()
            vm.navEvents.test {
                vm.onItemClicked(MediaItem.MovieItem(MOVIE))
                val event = awaitItem() as HomeNavEvent.NavigateToDetail
                assertEquals(1, event.mediaId)
                assertEquals("movie", event.mediaType)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN tv item WHEN onItemClicked THEN emits NavigateToDetail with tv type`() = runTest {
            val vm = createViewModel()
            vm.navEvents.test {
                vm.onItemClicked(MediaItem.TvItem(TV_SHOW))
                val event = awaitItem() as HomeNavEvent.NavigateToDetail
                assertEquals(10, event.mediaId)
                assertEquals("tv", event.mediaType)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class Pagination {

        private val PAGE_2_MOVIE = Movie(99, "Page 2 Movie", null, null, "", 6.0, "2024-03-01")

        @Test
        fun `GIVEN page 1 loaded WHEN loadMore trending THEN appends new data to existing list`() = runTest {
            every { getTrending(2) } returns flowOf(Result.Success(listOf(PAGE_2_MOVIE)))
            val vm = createViewModel()

            vm.loadMore(HomeCategory.TRENDING)

            vm.state.test {
                val state = awaitItem()
                assertEquals(2, state.trending.items.size)
                assertEquals(MOVIE, state.trending.items[0])
                assertEquals(PAGE_2_MOVIE, state.trending.items[1])
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN page 1 loaded WHEN loadMore trending THEN page increments to 2`() = runTest {
            every { getTrending(2) } returns flowOf(Result.Success(listOf(PAGE_2_MOVIE)))
            val vm = createViewModel()

            vm.loadMore(HomeCategory.TRENDING)

            vm.state.test {
                val state = awaitItem()
                assertEquals(2, state.trending.page)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN empty result from page 2 WHEN loadMore trending THEN canLoadMore becomes false`() = runTest {
            every { getTrending(2) } returns flowOf(Result.Success(emptyList()))
            val vm = createViewModel()

            vm.loadMore(HomeCategory.TRENDING)

            vm.state.test {
                val state = awaitItem()
                assertFalse(state.trending.canLoadMore)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN canLoadMore is false WHEN loadMore trending THEN state unchanged`() = runTest {
            every { getTrending(2) } returns flowOf(Result.Success(emptyList()))
            val vm = createViewModel()
            vm.loadMore(HomeCategory.TRENDING)

            vm.loadMore(HomeCategory.TRENDING)

            verify(exactly = 0) { getTrending(3) }
        }
    }
}
