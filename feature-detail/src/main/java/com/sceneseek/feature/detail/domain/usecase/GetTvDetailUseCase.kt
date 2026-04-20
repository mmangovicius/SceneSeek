package com.sceneseek.feature.detail.domain.usecase

import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTvDetailUseCase @Inject constructor(private val repository: TvRepository) {
    operator fun invoke(id: Int): Flow<Result<TvShow>> = repository.getTvDetail(id)
}
