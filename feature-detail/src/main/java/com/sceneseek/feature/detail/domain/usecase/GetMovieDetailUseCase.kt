package com.sceneseek.feature.detail.domain.usecase

import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMovieDetailUseCase @Inject constructor(private val repository: MovieRepository) {
    operator fun invoke(id: Int): Flow<Result<Movie>> = repository.getMovieDetail(id)
}
