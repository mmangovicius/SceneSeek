package com.sceneseek.feature.detail.domain.usecase

import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTrailersUseCase @Inject constructor(
    private val movieRepository: MovieRepository,
    private val tvRepository: TvRepository,
) {
    operator fun invoke(id: Int, mediaType: MediaType): Flow<Result<List<Trailer>>> =
        when (mediaType) {
            is MediaType.Movie -> movieRepository.getTrailers(id)
            is MediaType.TvShow -> tvRepository.getTvTrailers(id)
        }
}
