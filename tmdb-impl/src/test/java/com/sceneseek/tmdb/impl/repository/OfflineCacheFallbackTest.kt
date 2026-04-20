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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import retrofit2.Response
import java.io.IOException

private val MOVIE_DTO = MovieDto(1, "Cached Movie", "/p.jpg", null, "Overview", 7.5, "2024-01-01")

@ExtendWith(MockKExtension::class, UnconfinedTestDispatcherExtension::class)
internal class OfflineCacheFallbackTest {

    private val movieService = mockk<TmdbMovieService>()
    private val movieDao = mockk<MovieDao>(relaxed = true)
    private lateinit var repository: RemoteMovieRepository

    @BeforeEach
    fun setUp() {
        repository = RemoteMovieRepository(movieService, movieDao, TestDispatcherProvider())
    }

    @Nested
    inner class WhenNetworkFails {

        @Test
        fun `GIVEN IOException WHEN getPopularMovies THEN emits Error`() = runTest {
            coEvery { movieService.getPopular(any()) } throws IOException("No network")
            every { movieDao.getByCategory(any()) } returns flowOf(emptyList())

            repository.getPopularMovies(1).test {
                assertTrue(awaitItem() is Result.Loading)
                assertTrue(awaitItem() is Result.Error)
                awaitComplete()
            }
        }
    }

    @Nested
    inner class WhenNetworkSucceeds {

        @Test
        fun `GIVEN successful response WHEN getPopularMovies THEN emits Success and writes to cache`() = runTest {
            coEvery { movieService.getPopular(any()) } returns
                Response.success(PagedResponse(1, listOf(MOVIE_DTO), 1, 1))

            repository.getPopularMovies(1).test {
                assertTrue(awaitItem() is Result.Loading)
                assertTrue(awaitItem() is Result.Success)
                awaitComplete()
            }

            coVerify { movieDao.replaceByCategory(any(), any()) }
        }
    }
}
