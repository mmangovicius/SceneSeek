package com.sceneseek.tmdb.impl.repository

import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.moviestorage.CacheCategory
import com.sceneseek.moviestorage.dao.MovieDao
import com.sceneseek.moviestorage.entity.MovieEntity
import com.sceneseek.tmdb.api.service.TmdbMovieService
import com.sceneseek.core.di.DispatcherProvider
import com.sceneseek.tmdb.api.dto.MovieDto
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

class RemoteMovieRepository @Inject constructor(
    private val movieService: TmdbMovieService,
    private val movieDao: MovieDao,
    private val dispatchers: DispatcherProvider,
) : MovieRepository {

    override fun getPopularMovies(page: Int): Flow<Result<List<Movie>>> =
        fetchMovies(CacheCategory.POPULAR) { movieService.getPopular(page) }

    override fun getTrendingMovies(page: Int): Flow<Result<List<Movie>>> =
        fetchMovies(CacheCategory.TRENDING) { movieService.getTrending() }

    override fun getTopRatedMovies(page: Int): Flow<Result<List<Movie>>> =
        fetchMovies(CacheCategory.TOP_RATED) { movieService.getTopRated(page) }

    private fun fetchMovies(
        category: String,
        apiCall: suspend () -> Response<PagedResponse<MovieDto>>,
    ): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        try {
            val result = apiCall().toResult()
            when (result) {
                is Result.Success -> {
                    val dtos = result.data.results
                    withContext(dispatchers.io) {
                        movieDao.replaceByCategory(category, dtos.map { it.toEntity(category) })
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

    private suspend fun FlowCollector<Result<List<Movie>>>.emitCachedOrError(
        category: String,
        throwable: Throwable,
    ) {
        val cached = withContext(dispatchers.io) { movieDao.getByCategory(category).first() }
        if (cached.isNotEmpty()) {
            emit(Result.Success(cached.map { it.toDomain() }))
        } else {
            emit(Result.Error(throwable))
        }
    }

    override fun getMovieDetail(id: Int): Flow<Result<Movie>> = safeFlow {
        emit(Result.Loading)
        try {
            val result = movieService.getMovieDetail(id).toResult()
            when (result) {
                is Result.Success -> emit(Result.Success(result.data.toDomain()))
                is Result.Error -> {
                    val cached = withContext(dispatchers.io) { movieDao.getById(id) }
                    if (cached != null) emit(Result.Success(cached.toDomain()))
                    else emit(Result.Error(result.throwable))
                }

                else -> {}
            }
        } catch (e: Exception) {
            val cached = withContext(dispatchers.io) { movieDao.getById(id) }
            if (cached != null) emit(Result.Success(cached.toDomain()))
            else emit(Result.Error(e))
        }
    }

    override fun getCredits(id: Int): Flow<Result<List<Cast>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getCredits(id).toResult()
        when (result) {
            is Result.Success -> emit(
                Result.Success(
                    result.data.cast.map {
                        Cast(
                            it.id,
                            it.name,
                            it.character,
                            it.profilePath
                        )
                    }
                )
            )

            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTrailers(id: Int): Flow<Result<List<Trailer>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getVideos(id).toResult()
        when (result) {
            is Result.Success -> emit(
                Result.Success(
                    result.data.results.map {
                        Trailer(it.key, it.name, it.site, it.type)
                    }
                )
            )

            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getSimilarMovies(id: Int, page: Int): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getSimilar(id, page).toResult()
        when (result) {
            is Result.Success -> emit(
                Result.Success(
                    result.data.results.map { dto ->
                        Movie(
                            dto.id,
                            dto.title,
                            dto.posterPath,
                            dto.backdropPath,
                            dto.overview,
                            dto.voteAverage,
                            dto.releaseDate
                        )
                    }
                )
            )

            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    private fun MovieEntity.toDomain() =
        Movie(id, title, posterPath, backdropPath, overview, voteAverage, releaseDate)

    private fun MovieDto.toDomain() =
        Movie(id, title, posterPath, backdropPath, overview, voteAverage, releaseDate)

    private fun MovieDto.toEntity(category: String) = MovieEntity(
        id,
        title,
        overview,
        posterPath,
        backdropPath,
        voteAverage,
        releaseDate,
        category = category
    )
}
