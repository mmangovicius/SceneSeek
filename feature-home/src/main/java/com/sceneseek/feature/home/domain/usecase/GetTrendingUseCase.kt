package com.sceneseek.feature.home.domain.usecase

import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrendingUseCase @Inject constructor(private val repository: MovieRepository) {
    operator fun invoke(): Flow<Result<List<Movie>>> = repository.getTrendingMovies()
}
