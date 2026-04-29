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
        fetchMovies(
            category = CacheCategory.POPULAR,
            page = page,
        ) { movieService.getPopular(page) }

    override fun getTrendingMovies(page: Int): Flow<Result<List<Movie>>> =
        fetchMovies(
            category = CacheCategory.TRENDING,
            page = page,
        ) { movieService.getTrending(page = page) }

    override fun getTopRatedMovies(page: Int): Flow<Result<List<Movie>>> =
        fetchMovies(
            category = CacheCategory.TOP_RATED,
            page = page,
        ) { movieService.getTopRated(page) }

    private fun fetchMovies(
        category: String,
        page: Int = 1,
        apiCall: suspend () -> Response<PagedResponse<MovieDto>>,
    ): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        try {
            val result = apiCall().toResult()
            when (result) {
                is Result.Success -> {
                    val dtos = result.data.results
                    if (page == 1) {
                        withContext(dispatchers.io) {
                            movieDao.replaceByCategory(category, dtos.map { it.toEntity(category) })
                        }
                    }
                    emit(Result.Success(dtos.map { it.toDomain() }))
                }

                is Result.Error -> if (page == 1) emitCachedOrError(category, result.throwable) else emit(Result.Error(result.throwable))
                else -> {}
            }
        } catch (e: Exception) {
            if (page == 1) emitCachedOrError(category, e) else emit(Result.Error(e))
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
                            id = it.id,
                            name = it.name,
                            character = it.character,
                            profilePath = it.profilePath,
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
                        Trailer(
                            key = it.key,
                            name = it.name,
                            site = it.site,
                            type = it.type,
                        )
                    }
                )
            )

            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getSimilarMovies(id: Int, page: Int): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getSimilar(
            id = id,
            page = page,
        ).toResult()
        when (result) {
            is Result.Success -> emit(
                Result.Success(
                    result.data.results.map { dto ->
                        Movie(
                            id = dto.id,
                            title = dto.title,
                            posterPath = dto.posterPath,
                            backdropPath = dto.backdropPath,
                            overview = dto.overview,
                            voteAverage = dto.voteAverage,
                            releaseDate = dto.releaseDate,
                        )
                    }
                )
            )

            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    private fun MovieEntity.toDomain() = Movie(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        overview = overview,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
    )

    private fun MovieDto.toDomain() = Movie(
        id = id,
        title = title,
        posterPath = posterPath,
        backdropPath = backdropPath,
        overview = overview,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
    )

    private fun MovieDto.toEntity(category: String) = MovieEntity(
        id = id,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        releaseDate = releaseDate,
        popularity = popularity,
        category = category
    )
}
