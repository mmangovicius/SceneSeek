package com.sceneseek.core.domain.repository

import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getPopularMovies(page: Int = 1): Flow<Result<List<Movie>>>
    fun getTrendingMovies(page: Int = 1): Flow<Result<List<Movie>>>
    fun getTopRatedMovies(page: Int = 1): Flow<Result<List<Movie>>>
    fun getMovieDetail(id: Int): Flow<Result<Movie>>
    fun getCredits(id: Int): Flow<Result<List<Cast>>>
    fun getTrailers(id: Int): Flow<Result<List<Trailer>>>
    fun getSimilarMovies(id: Int, page: Int = 1): Flow<Result<List<Movie>>>
}
