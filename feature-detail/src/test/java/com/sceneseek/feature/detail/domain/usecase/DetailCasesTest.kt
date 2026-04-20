package com.sceneseek.feature.detail.domain.usecase

import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.util.Result
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

private val MOVIE_FLOW = flowOf(Result.Success(Movie(1, "M", null, null, "", 7.0, "")))
private val TV_FLOW = flowOf(Result.Success(TvShow(1, "S", null, null, "", 7.0, "")))
private val CAST_FLOW = flowOf(Result.Success(emptyList<Cast>()))
private val TRAILER_FLOW = flowOf(Result.Success(emptyList<Trailer>()))

@ExtendWith(MockKExtension::class)
internal class DetailCasesTest {

    private val movieRepo = mockk<MovieRepository>()
    private val tvRepo = mockk<TvRepository>()

    @Nested
    inner class GetMovieDetailUseCaseTest {
        private lateinit var useCase: GetMovieDetailUseCase

        @BeforeEach
        fun setUp() { useCase = GetMovieDetailUseCase(movieRepo) }

        @Test
        fun `GIVEN id 1 WHEN invoke THEN delegates to movieRepository getMovieDetail`() {
            every { movieRepo.getMovieDetail(1) } returns MOVIE_FLOW
            assertSame(MOVIE_FLOW, useCase(1))
        }
    }

    @Nested
    inner class GetTvDetailUseCaseTest {
        private lateinit var useCase: GetTvDetailUseCase

        @BeforeEach
        fun setUp() { useCase = GetTvDetailUseCase(tvRepo) }

        @Test
        fun `GIVEN id 1 WHEN invoke THEN delegates to tvRepository getTvDetail`() {
            every { tvRepo.getTvDetail(1) } returns TV_FLOW
            assertSame(TV_FLOW, useCase(1))
        }
    }

    @Nested
    inner class GetCreditsUseCaseTest {
        private lateinit var useCase: GetCreditsUseCase

        @BeforeEach
        fun setUp() { useCase = GetCreditsUseCase(movieRepo, tvRepo) }

        @Test
        fun `GIVEN Movie type WHEN invoke THEN delegates to movieRepository getCredits`() {
            every { movieRepo.getCredits(1) } returns CAST_FLOW
            assertSame(CAST_FLOW, useCase(1, MediaType.Movie))
        }

        @Test
        fun `GIVEN TvShow type WHEN invoke THEN delegates to tvRepository getTvCredits`() {
            every { tvRepo.getTvCredits(1) } returns CAST_FLOW
            assertSame(CAST_FLOW, useCase(1, MediaType.TvShow))
        }
    }

    @Nested
    inner class GetTrailersUseCaseTest {
        private lateinit var useCase: GetTrailersUseCase

        @BeforeEach
        fun setUp() { useCase = GetTrailersUseCase(movieRepo, tvRepo) }

        @Test
        fun `GIVEN Movie type WHEN invoke THEN delegates to movieRepository getTrailers`() {
            every { movieRepo.getTrailers(1) } returns TRAILER_FLOW
            assertSame(TRAILER_FLOW, useCase(1, MediaType.Movie))
        }

        @Test
        fun `GIVEN TvShow type WHEN invoke THEN delegates to tvRepository getTvTrailers`() {
            every { tvRepo.getTvTrailers(1) } returns TRAILER_FLOW
            assertSame(TRAILER_FLOW, useCase(1, MediaType.TvShow))
        }
    }
}
