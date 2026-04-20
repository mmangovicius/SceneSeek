package com.sceneseek.feature.watchlist.presentation

import app.cash.turbine.test
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.WatchlistItem
import com.sceneseek.core.domain.repository.WatchlistRepository
import com.sceneseek.feature.watchlist.domain.usecase.ToggleWatchlistUseCase
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

private val MOVIE_ITEM = WatchlistItem(1, MediaType.Movie, "Test Movie", null, 0L)
private val TV_ITEM = WatchlistItem(2, MediaType.TvShow, "Test Show", "/p.jpg", 0L)

@ExtendWith(MockKExtension::class, UnconfinedTestDispatcherExtension::class)
internal class WatchlistViewModelTest {

    private val repository = mockk<WatchlistRepository>(relaxed = true)
    private val toggleUseCase = mockk<ToggleWatchlistUseCase>(relaxed = true)

    private fun createViewModel() = WatchlistViewModel(repository, toggleUseCase)

    @Nested
    inner class WhenWatchlistIsEmpty {

        @BeforeEach
        fun setUp() {
            every { repository.getAll() } returns flowOf(emptyList())
        }

        @Test
        fun `GIVEN empty repository WHEN initialized THEN isEmpty is true`() = runTest {
            createViewModel().state.test {
                assertTrue(awaitItem().isEmpty)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN empty repository WHEN initialized THEN items list is empty`() = runTest {
            createViewModel().state.test {
                assertTrue(awaitItem().items.isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class WhenWatchlistHasItems {

        @BeforeEach
        fun setUp() {
            every { repository.getAll() } returns flowOf(listOf(MOVIE_ITEM, TV_ITEM))
        }

        @Test
        fun `GIVEN items in repository WHEN initialized THEN isEmpty is false`() = runTest {
            createViewModel().state.test {
                assertFalse(awaitItem().isEmpty)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN items in repository WHEN initialized THEN state has correct item count`() = runTest {
            createViewModel().state.test {
                assertEquals(2, awaitItem().items.size)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    inner class OnItemInteraction {

        @BeforeEach
        fun setUp() {
            every { repository.getAll() } returns flowOf(emptyList())
        }

        @Test
        fun `GIVEN movie item WHEN onItemRemoved THEN calls toggleUseCase`() = runTest {
            val vm = createViewModel()
            vm.onItemRemoved(MOVIE_ITEM)
            coVerify { toggleUseCase(MOVIE_ITEM) }
        }

        @Test
        fun `GIVEN movie item WHEN onItemClicked THEN emits NavigateToDetail with movie type`() = runTest {
            val vm = createViewModel()
            vm.navEvents.test {
                vm.onItemClicked(MOVIE_ITEM)
                val event = awaitItem() as WatchlistNavEvent.NavigateToDetail
                assertEquals(1, event.mediaId)
                assertEquals("movie", event.mediaType)
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `GIVEN tv item WHEN onItemClicked THEN emits NavigateToDetail with tv type`() = runTest {
            val vm = createViewModel()
            vm.navEvents.test {
                vm.onItemClicked(TV_ITEM)
                val event = awaitItem() as WatchlistNavEvent.NavigateToDetail
                assertEquals(2, event.mediaId)
                assertEquals("tv", event.mediaType)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
