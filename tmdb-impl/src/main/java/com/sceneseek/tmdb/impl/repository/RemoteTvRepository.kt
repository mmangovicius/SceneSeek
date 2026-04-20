package com.sceneseek.tmdb.impl.repository

import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.TvRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.moviestorage.CacheCategory
import com.sceneseek.moviestorage.dao.TvShowDao
import com.sceneseek.moviestorage.entity.TvShowEntity
import com.sceneseek.tmdb.api.service.TmdbTvService
import com.sceneseek.core.di.DispatcherProvider
import com.sceneseek.tmdb.api.dto.TvShowDto
import com.sceneseek.tmdb.api.model.PagedResponse
import com.sceneseek.tmdb.impl.util.toResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject

private fun <T> safeFlow(block: suspend FlowCollector<Result<T>>.() -> Unit): Flow<Result<T>> =
    flow(block).catch { e -> emit(Result.Error(e)) }

class RemoteTvRepository @Inject constructor(
    private val tvService: TmdbTvService,
    private val tvShowDao: TvShowDao,
    private val dispatchers: DispatcherProvider,
) : TvRepository {

    override fun getPopularTv(page: Int): Flow<Result<List<TvShow>>> =
        fetchTvShows(CacheCategory.POPULAR) { tvService.getPopular(page) }

    override fun getTrendingTv(page: Int): Flow<Result<List<TvShow>>> =
        fetchTvShows(CacheCategory.TRENDING) { tvService.getTrending() }

    override fun getTopRatedTv(page: Int): Flow<Result<List<TvShow>>> =
        fetchTvShows(CacheCategory.TOP_RATED) { tvService.getTopRated(page) }

    private fun fetchTvShows(
        category: String,
        apiCall: suspend () -> Response<PagedResponse<TvShowDto>>,
    ): Flow<Result<List<TvShow>>> = safeFlow {
        emit(Result.Loading)
        try {
            val result = apiCall().toResult()
            when (result) {
                is Result.Success -> {
                    val dtos = result.data.results
                    withContext(dispatchers.io) {
                        tvShowDao.replaceByCategory(category, dtos.map { it.toEntity(category) })
                    }
                    emit(Result.Success(dtos.map { it.toDomain() }))
                }

                is Result.Error -> emitCachedOrError(category, result.throwable)
                else -> {}
            }
        } catch (e: Exception) {
            emitCachedOrError(category, e)
        }
    }

    private suspend fun FlowCollector<Result<List<TvShow>>>.emitCachedOrError(
        category: String,
        throwable: Throwable,
    ) {
        val cached = withContext(dispatchers.io) { tvShowDao.getByCategory(category).first() }
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.map { it.toDomain() }))
        } else {
            emit(Result.Error(throwable))
        }
    }

    override fun getTvDetail(id: Int): Flow<Result<TvShow>> = safeFlow {
        emit(Result.Loading)
        try {
            val result = tvService.getTvDetail(id).toResult()
            when (result) {
                is Result.Success -> emit(Result.Success(result.data.toDomain()))
                is Result.Error -> {
                    val cached = withContext(dispatchers.io) { tvShowDao.getById(id) }
                    if (cached != null) emit(Result.Success(cached.toDomain()))
                    else emit(Result.Error(result.throwable))
                }

                else -> {}
            }
        } catch (e: Exception) {
            val cached = withContext(dispatchers.io) { tvShowDao.getById(id) }
            if (cached != null) emit(Result.Success(cached.toDomain()))
            else emit(Result.Error(e))
        }
    }

    override fun getTvCredits(id: Int): Flow<Result<List<Cast>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getCredits(id).toResult()
        when (result) {
            is Result.Success -> emit(
                Result.Success(
                    result.data.cast.map {
                        Cast(it.id, it.name, it.character, it.profilePath)
                    }
                )
            )

            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTvTrailers(id: Int): Flow<Result<List<Trailer>>> = safeFlow {
        emit(Result.Loading)
        val result = tvService.getVideos(id).toResult()
        when (result) {
            is Result.Success -> emit(
                Result.Success(
                    result.data.results.map {
                        Trailer(
                            it.key,
                            it.name,
                            it.site,
                            it.type
                        )
                    })
            )

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

    private fun TvShowEntity.toDomain() =
        TvShow(id, name, posterPath, backdropPath, overview, voteAverage, firstAirDate)

    private fun TvShowDto.toDomain() =
        TvShow(id, name, posterPath, backdropPath, overview, voteAverage, firstAirDate)

    private fun TvShowDto.toEntity(category: String) = TvShowEntity(
        id = id,
        name = name,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        firstAirDate = firstAirDate,
        popularity = popularity,
        category = category
    )
}
