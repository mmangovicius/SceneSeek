package com.sceneseek.tmdb.impl.repository

import app.cash.turbine.test
import com.sceneseek.core.domain.util.Result
import com.sceneseek.moviestorage.dao.MovieDao
import com.sceneseek.tmdb.api.dto.MovieDto
import com.sceneseek.tmdb.api.model.PagedResponse
import com.sceneseek.tmdb.api.service.TmdbMovieService
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

private val MOVIE_DTO = MovieDto(1, "Test Movie", "/poster.jpg", null, "Overview", 7.5, "2024-01-01")

@ExtendWith(MockKExtension::class, UnconfinedTestDispatcherExtension::class)
internal class RemoteMovieRepositoryTest {

    private val movieService = mockk<TmdbMovieService>()
    private val movieDao = mockk<MovieDao>(relaxed = true)
    private lateinit var repository: RemoteMovieRepository

    @BeforeEach
    fun setUp() {
        repository = RemoteMovieRepository(movieService, movieDao, TestDispatcherProvider())
    }

    @Nested
    inner class WhenNetworkIsAvailable {

        @Test
        fun `GIVEN valid response WHEN getPopularMovies THEN emits Loading then Success with data`() = runTest {
            coEvery { movieService.getPopular(any()) } returns
                Response.success(PagedResponse(1, listOf(MOVIE_DTO), 1, 1))

            repository.getPopularMovies().test {
                assertTrue(awaitItem() is Result.Loading)
                val result = awaitItem()
                assertTrue(result is Result.Success)
                assertEquals("Test Movie", (result as Result.Success).data.first().title)
                awaitComplete()
            }
        }

        @Test
        fun `GIVEN valid response WHEN getPopularMovies THEN caches result in Room`() = runTest {
            coEvery { movieService.getPopular(any()) } returns
                Response.success(PagedResponse(1, listOf(MOVIE_DTO), 1, 1))

            repository.getPopularMovies().test { cancelAndIgnoreRemainingEvents() }

            coVerify { movieDao.replaceByCategory(any(), any()) }
        }

        @Test
        fun `GIVEN valid response WHEN getMovieDetail THEN emits Loading then Success with correct id`() = runTest {
            coEvery { movieService.getMovieDetail(1) } returns Response.success(MOVIE_DTO)

            repository.getMovieDetail(1).test {
                assertTrue(awaitItem() is Result.Loading)
                val result = awaitItem()
                assertTrue(result is Result.Success)
                assertEquals(1, (result as Result.Success).data.id)
                awaitComplete()
            }
        }
    }

    @Nested
    inner class WhenNetworkFails {

        @Test
        fun `GIVEN IOException WHEN getPopularMovies THEN emits Loading then Error`() = runTest {
            coEvery { movieService.getPopular(any()) } throws java.io.IOException("No network")
            every { movieDao.getByCategory(any()) } returns flowOf(emptyList())

            repository.getPopularMovies().test {
                assertTrue(awaitItem() is Result.Loading)
                assertTrue(awaitItem() is Result.Error)
                awaitComplete()
            }
        }

        @Test
        fun `GIVEN 401 response WHEN getPopularMovies THEN emits AuthException error`() = runTest {
            coEvery { movieService.getPopular(any()) } returns
                Response.error(401, okhttp3.ResponseBody.create(null, ""))
            every { movieDao.getByCategory(any()) } returns flowOf(emptyList())

            repository.getPopularMovies().test {
                assertTrue(awaitItem() is Result.Loading)
                val error = awaitItem() as Result.Error
                assertTrue(error.throwable.message?.contains("Unauthorized") == true)
                awaitComplete()
            }
        }
    }

    @Nested
    inner class WhenPaginating {

        @Test
        fun `GIVEN page 2 WHEN getPopularMovies THEN does not cache in Room`() = runTest {
            coEvery { movieService.getPopular(2) } returns
                Response.success(PagedResponse(2, listOf(MOVIE_DTO), 1, 2))

            repository.getPopularMovies(page = 2).test { cancelAndIgnoreRemainingEvents() }

            coVerify(exactly = 0) { movieDao.replaceByCategory(any(), any()) }
        }

        @Test
        fun `GIVEN page 2 and network failure WHEN getPopularMovies THEN emits Error without cache fallback`() = runTest {
            coEvery { movieService.getPopular(2) } throws java.io.IOException("No network")

            repository.getPopularMovies(page = 2).test {
                assertTrue(awaitItem() is Result.Loading)
                assertTrue(awaitItem() is Result.Error)
                awaitComplete()
            }

            coVerify(exactly = 0) { movieDao.getByCategory(any()) }
        }
    }
}
