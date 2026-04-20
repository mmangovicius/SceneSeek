package com.sceneseek.feature.watchlist.domain.usecase

import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.WatchlistItem
import com.sceneseek.core.domain.repository.WatchlistRepository
import io.mockk.coVerify
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private val MOVIE_ITEM = WatchlistItem(1, MediaType.Movie, "Test Movie", null, 0L)
private val TV_ITEM = WatchlistItem(2, MediaType.TvShow, "Test Show", null, 0L)

@ExtendWith(MockKExtension::class)
internal class ToggleWatchlistUseCaseTest {

    private val repository = mockk<WatchlistRepository>(relaxed = true)
    private lateinit var useCase: ToggleWatchlistUseCase

    @BeforeEach
    fun setUp() {
        useCase = ToggleWatchlistUseCase(repository)
    }

    @Nested
    inner class WhenTogglingMovie {

        @Test
        fun `GIVEN movie item WHEN invoke THEN delegates to repository toggle`() = runTest {
            useCase(MOVIE_ITEM)
            coVerify { repository.toggle(MOVIE_ITEM) }
        }
    }

    @Nested
    inner class WhenTogglingTvShow {

        @Test
        fun `GIVEN tv show item WHEN invoke THEN delegates to repository toggle`() = runTest {
            useCase(TV_ITEM)
            coVerify { repository.toggle(TV_ITEM) }
        }
    }
}
