package com.sceneseek.tmdb.impl.repository

import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.moviestorage.dao.TvShowDao
import com.sceneseek.moviestorage.entity.TvShowEntity
import com.sceneseek.tmdb.api.service.TmdbTvService
import com.sceneseek.core.di.DispatcherProvider
import com.sceneseek.tmdb.impl.util.toResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

private fun <T> safeFlow(block: suspend kotlinx.coroutines.flow.FlowCollector<Result<T>>.() -> Unit): Flow<Result<T>> =
    flow(block).catch { e -> emit(Result.Error(e)) }

class RemoteTvRepository @Inject constructor(
    private val tvService: TmdbTvService,
    private val tvShowDao: TvShowDao,
    private val dispatchers: DispatcherProvider,
) : TvRepository {

    override fun getPopularTv(page: Int): Flow<Result<List<TvShow>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getPopular(page).toResult()
        when (result) {
            is Result.Success -> {
                val dtos = result.data.results
                withContext(dispatchers.io) {
                    tvShowDao.replaceAll(dtos.map { it.toEntity() })
                }
                emit(Result.Success(dtos.map { it.toDomain() }))
            }
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTrendingTv(page: Int): Flow<Result<List<TvShow>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getTrending().toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { it.toDomain() }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTopRatedTv(page: Int): Flow<Result<List<TvShow>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getTopRated(page).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { it.toDomain() }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTvDetail(id: Int): Flow<Result<TvShow>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getTvDetail(id).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.toDomain()))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTvCredits(id: Int): Flow<Result<List<Cast>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getCredits(id).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.cast.map { Cast(it.id, it.name, it.character, it.profilePath) }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTvTrailers(id: Int): Flow<Result<List<Trailer>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getVideos(id).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { Trailer(it.key, it.name, it.site, it.type) }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getSimilarTv(id: Int, page: Int): Flow<Result<List<TvShow>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getSimilar(id, page).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { it.toDomain() }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    private fun TvShowEntity.toDomain() = TvShow(id, name, posterPath, backdropPath, overview, voteAverage, firstAirDate)
    private fun com.sceneseek.tmdb.api.dto.TvShowDto.toDomain() = TvShow(id, name, posterPath, backdropPath, overview, voteAverage, firstAirDate)
    private fun com.sceneseek.tmdb.api.dto.TvShowDto.toEntity() = TvShowEntity(id, name, overview, posterPath, backdropPath, voteAverage, firstAirDate)
}
