package com.sceneseek.tmdb.impl.repository

import app.cash.turbine.test
import com.sceneseek.core.domain.util.Result
import com.sceneseek.moviestorage.dao.TvShowDao
import com.sceneseek.tmdb.api.dto.TvShowDto
import com.sceneseek.tmdb.api.model.PagedResponse
import com.sceneseek.tmdb.api.service.TmdbTvService
import com.sceneseek.testutils.TestDispatcherProvider
import com.sceneseek.testutils.UnconfinedTestDispatcherExtension
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import retrofit2.Response

private val TV_DTO = TvShowDto(1, "Test Show", "/poster.jpg", null, "Overview", 8.0, "2024-01-01")

@ExtendWith(MockKExtension::class, UnconfinedTestDispatcherExtension::class)
internal class RemoteTvRepositoryTest {

    private val tvService = mockk<TmdbTvService>()
    private val tvShowDao = mockk<TvShowDao>(relaxed = true)
    private lateinit var repository: RemoteTvRepository

    @BeforeEach
    fun setUp() {
        repository = RemoteTvRepository(tvService, tvShowDao, TestDispatcherProvider())
    }

    @Nested
    inner class WhenNetworkIsAvailable {

        @Test
        fun `GIVEN valid response WHEN getPopularTv THEN emits Loading then Success`() = runTest {
            coEvery { tvService.getPopular(any()) } returns
                Response.success(PagedResponse(1, listOf(TV_DTO), 1, 1))

            repository.getPopularTv().test {
                assertTrue(awaitItem() is Result.Loading)
                val result = awaitItem()
                assertTrue(result is Result.Success)
                assertEquals("Test Show", (result as Result.Success).data.first().name)
                awaitComplete()
            }
        }

        @Test
        fun `GIVEN valid response WHEN getPopularTv THEN caches result in Room`() = runTest {
            coEvery { tvService.getPopular(any()) } returns
                Response.success(PagedResponse(1, listOf(TV_DTO), 1, 1))

            repository.getPopularTv().test { cancelAndIgnoreRemainingEvents() }

            coVerify { tvShowDao.replaceByCategory(any(), any()) }
        }
    }

    @Nested
    inner class WhenNetworkFails {

        @Test
        fun `GIVEN IOException WHEN getPopularTv THEN emits Loading then Error`() = runTest {
            coEvery { tvService.getPopular(any()) } throws java.io.IOException("No network")
            every { tvShowDao.getByCategory(any()) } returns flowOf(emptyList())

            repository.getPopularTv().test {
                assertTrue(awaitItem() is Result.Loading)
                assertTrue(awaitItem() is Result.Error)
                awaitComplete()
            }
        }
    }

    @Nested
    inner class WhenPaginating {

        @Test
        fun `GIVEN page 2 WHEN getPopularTv THEN does not cache in Room`() = runTest {
            coEvery { tvService.getPopular(2) } returns
                Response.success(PagedResponse(2, listOf(TV_DTO), 1, 2))

            repository.getPopularTv(page = 2).test { cancelAndIgnoreRemainingEvents() }

            coVerify(exactly = 0) { tvShowDao.replaceByCategory(any(), any()) }
        }

        @Test
        fun `GIVEN page 2 and network failure WHEN getPopularTv THEN emits Error without cache fallback`() = runTest {
            coEvery { tvService.getPopular(2) } throws java.io.IOException("No network")

            repository.getPopularTv(page = 2).test {
                assertTrue(awaitItem() is Result.Loading)
                assertTrue(awaitItem() is Result.Error)
                awaitComplete()
            }

            coVerify(exactly = 0) { tvShowDao.getByCategory(any()) }
        }
    }
}
