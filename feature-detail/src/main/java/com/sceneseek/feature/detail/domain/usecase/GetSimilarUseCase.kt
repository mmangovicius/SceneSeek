package com.sceneseek.feature.detail.domain.usecase

import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetSimilarUseCase @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvRepository: TvRepository,
) {
    operator fun invoke(id: Int, mediaType: MediaType, page: Int = 1): Flow<Result<List<MediaItem>>> =
        when (mediaType) {
            is MediaType.Movie -> movieRepository.getSimilarMovies(id, page).map { result ->
                when (result) {
                    is Result.Success -> Result.Success(result.data.map { MediaItem.MovieItem(it) })
                    is Result.Error -> Result.Error(result.throwable)
                    is Result.Loading -> Result.Loading
                }
            }
            is MediaType.TvShow -> tvRepository.getSimilarTv(id, page).map { result ->
                when (result) {
                    is Result.Success -> Result.Success(result.data.map { MediaItem.TvItem(it) })
                    is Result.Error -> Result.Error(result.throwable)
                    is Result.Loading -> Result.Loading
                }
            }
        }
}
