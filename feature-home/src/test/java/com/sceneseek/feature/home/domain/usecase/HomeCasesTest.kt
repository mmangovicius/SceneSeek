package com.sceneseek.feature.home.domain.usecase

import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.util.Result
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private val MOVIE_FLOW = flowOf(Result.Success(emptyList<Movie>()))
private val TV_FLOW = flowOf(Result.Success(emptyList<TvShow>()))

@ExtendWith(MockKExtension::class)
internal class HomeCasesTest {

    private val movieRepo = mockk<MovieRepository>()
    private val tvRepo = mockk<TvRepository>()

    @Nested
    inner class GetTrendingUseCaseTest {
        private lateinit var useCase: GetTrendingUseCase

        @BeforeEach
        fun setUp() { useCase = GetTrendingUseCase(movieRepo) }

        @Test
        fun `GIVEN movie repository WHEN invoke THEN delegates to getTrendingMovies`() {
            every { movieRepo.getTrendingMovies(any()) } returns MOVIE_FLOW
            assertSame(MOVIE_FLOW, useCase())
            verify { movieRepo.getTrendingMovies(any()) }
        }
    }

    @Nested
    inner class GetPopularMoviesUseCaseTest {
        private lateinit var useCase: GetPopularMoviesUseCase

        @BeforeEach
        fun setUp() { useCase = GetPopularMoviesUseCase(movieRepo) }

        @Test
        fun `GIVEN page 1 WHEN invoke THEN delegates to getPopularMovies`() {
            every { movieRepo.getPopularMovies(1) } returns MOVIE_FLOW
            assertSame(MOVIE_FLOW, useCase(1))
        }
    }

    @Nested
    inner class GetPopularTvUseCaseTest {
        private lateinit var useCase: GetPopularTvUseCase

        @BeforeEach
        fun setUp() { useCase = GetPopularTvUseCase(tvRepo) }

        @Test
        fun `GIVEN tv repository WHEN invoke THEN delegates to getPopularTv`() {
            every { tvRepo.getPopularTv(1) } returns TV_FLOW
            assertSame(TV_FLOW, useCase(1))
        }
    }

    @Nested
    inner class GetTopRatedMoviesUseCaseTest {
        private lateinit var useCase: GetTopRatedMoviesUseCase

        @BeforeEach
        fun setUp() { useCase = GetTopRatedMoviesUseCase(movieRepo) }

        @Test
        fun `GIVEN page 1 WHEN invoke THEN delegates to getTopRatedMovies`() {
            every { movieRepo.getTopRatedMovies(1) } returns MOVIE_FLOW
            assertSame(MOVIE_FLOW, useCase(1))
        }
    }

    @Nested
    inner class GetTopRatedTvUseCaseTest {
        private lateinit var useCase: GetTopRatedTvUseCase

        @BeforeEach
        fun setUp() { useCase = GetTopRatedTvUseCase(tvRepo) }

        @Test
        fun `GIVEN tv repository WHEN invoke THEN delegates to getTopRatedTv`() {
            every { tvRepo.getTopRatedTv(1) } returns TV_FLOW
            assertSame(TV_FLOW, useCase(1))
        }
    }
}
