package com.sceneseek.core.domain.repository

import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface TvRepository {
    fun getPopularTv(page: Int = 1): Flow<Result<List<TvShow>>>
    fun getTrendingTv(page: Int = 1): Flow<Result<List<TvShow>>>
    fun getTopRatedTv(page: Int = 1): Flow<Result<List<TvShow>>>
    fun getTvDetail(id: Int): Flow<Result<TvShow>>
    fun getTvCredits(id: Int): Flow<Result<List<Cast>>>
    fun getTvTrailers(id: Int): Flow<Result<List<Trailer>>>
    fun getSimilarTv(id: Int, page: Int = 1): Flow<Result<List<TvShow>>>
}
