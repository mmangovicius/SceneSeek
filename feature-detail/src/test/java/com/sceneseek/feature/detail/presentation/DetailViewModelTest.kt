package com.sceneseek.feature.detail.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.repository.WatchlistRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.feature.detail.domain.usecase.GetCreditsUseCase
import com.sceneseek.feature.detail.domain.usecase.GetMovieDetailUseCase
import com.sceneseek.feature.detail.domain.usecase.GetSimilarUseCase
import com.sceneseek.feature.detail.domain.usecase.GetTrailersUseCase
import com.sceneseek.feature.detail.domain.usecase.GetTvDetailUseCase
import com.sceneseek.testutils.UnconfinedTestDispatcherExtension
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private const val MEDIA_ID = 1
private const val MEDIA_TYPE_MOVIE = "movie"
private val TEST_MOVIE = Movie(MEDIA_ID, "Test Movie", "/p.jpg", "/b.jpg", "Overview", 8.0, "2024-01-01")
private val TEST_CAST = listOf(Cast(1, "Actor", "Character", null))
private val TEST_TRAILER = Trailer("abc123", "Official Trailer", "YouTube", "Trailer")
private val TEST_SIMILAR = listOf(MediaItem.MovieItem(Movie(2, "Similar Movie", null, null, "", 7.0, "")))

@ExtendWith(MockKExtension::class, UnconfinedTestDispatcherExtension::class)
internal class DetailViewModelTest {

    private val savedStateHandle = SavedStateHandle(mapOf("mediaId" to MEDIA_ID, "mediaType" to MEDIA_TYPE_MOVIE))
    private val getMovieDetail = mockk<GetMovieDetailUseCase>()
    private val getTvDetail = mockk<GetTvDetailUseCase>()
    private val getCredits = mockk<GetCreditsUseCase>()
    private val getTrailers = mockk<GetTrailersUseCase>()
    private val getSimilar = mockk<GetSimilarUseCase>()
    private val watchlistRepository = mockk<WatchlistRepository>(relaxed = true)

    private fun createViewModel() = DetailViewModel(
        savedStateHandle, getMovieDetail, getTvDetail, getCredits, getTrailers, getSimilar, watchlistRepository
    )

    @BeforeEach
    fun setUp() {
        every { getMovieDetail(MEDIA_ID) } returns flowOf(Result.Success(TEST_MOVIE))
        every { getCredits(MEDIA_ID, MediaType.Movie) } returns flowOf(Result.Success(TEST_CAST))
        every { getTrailers(MEDIA_ID, MediaType.Movie) } returns flowOf(Result.Success(listOf(TEST_TRAILER)))
        every { getSimilar(MEDIA_ID, MediaType.Movie) } returns flowOf(Result.Success(TEST_SIMILAR))
        every { watchlistRepository.isWatchlisted(MEDIA_ID, MediaType.Movie) } returns flowOf(false)
    }

    @Nested
    inner class WhenLoadingMovieDetail {

        @Test
        fun `GIVEN valid use cases WHEN initialized THEN state has movie detail`() = runTest {
            createViewModel().state.test {
                val state = awaitItem()
                assertFalse(state.isLoading)
                assertEquals(TEST_MOVIE, state.movie)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN valid use cases WHEN initialized THEN state has cast`() = runTest {
            createViewModel().state.test {
                assertEquals(TEST_CAST, awaitItem().cast)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN valid use cases WHEN initialized THEN state has trailers`() = runTest {
            createViewModel().state.test {
                assertEquals(listOf(TEST_TRAILER), awaitItem().trailers)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN valid use cases WHEN initialized THEN state has similar items`() = runTest {
            createViewModel().state.test {
                assertEquals(TEST_SIMILAR, awaitItem().similar)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class WhenWatchlistInteraction {

        @Test
        fun `GIVEN item is watchlisted WHEN initialized THEN isWatchlisted is true`() = runTest {
            every { watchlistRepository.isWatchlisted(MEDIA_ID, MediaType.Movie) } returns flowOf(true)

            createViewModel().state.test {
                assertTrue(awaitItem().isWatchlisted)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN loaded detail WHEN onWatchlistToggled THEN calls repository toggle`() = runTest {
            val vm = createViewModel()
            vm.onWatchlistToggled()
            coVerify { watchlistRepository.toggle(any()) }
        }
    }

    @Nested
    inner class OnNavigation {

        @Test
        fun `GIVEN trailer WHEN onTrailerClicked THEN emits TrailerClicked with YouTube URL`() = runTest {
            val vm = createViewModel()
            vm.navEvents.test {
                vm.onTrailerClicked(TEST_TRAILER)
                val event = awaitItem() as DetailNavEvent.TrailerClicked
                assertTrue(event.url.contains("abc123"))
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN similar item WHEN onSimilarItemClicked THEN emits NavigateToDetail`() = runTest {
            val vm = createViewModel()
            vm.navEvents.test {
                vm.onSimilarItemClicked(TEST_SIMILAR.first())
                val event = awaitItem() as DetailNavEvent.NavigateToDetail
                assertEquals(2, event.mediaId)
                assertEquals("movie", event.mediaType)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
