package com.sceneseek.tmdb.impl.repository

import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.repository.MovieRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.moviestorage.dao.MovieDao
import com.sceneseek.moviestorage.entity.MovieEntity
import com.sceneseek.tmdb.api.service.TmdbMovieService
import com.sceneseek.core.di.DispatcherProvider
import com.sceneseek.tmdb.impl.util.toResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

private fun <T> safeFlow(block: suspend kotlinx.coroutines.flow.FlowCollector<Result<T>>.() -> Unit): Flow<Result<T>> =
    flow(block).catch { e -> emit(Result.Error(e)) }

class RemoteMovieRepository @Inject constructor(
    private val movieService: TmdbMovieService,
    private val movieDao: MovieDao,
    private val dispatchers: DispatcherProvider,
) : MovieRepository {

    override fun getPopularMovies(page: Int): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        try {
            val result = movieService.getPopular(page).toResult()
            when (result) {
                is Result.Success -> {
                    val dtos = result.data.results
                    withContext(dispatchers.io) {
                        movieDao.replaceAll(dtos.map { it.toEntity() })
                    }
                    emit(Result.Success(dtos.map { dto ->
                        Movie(dto.id, dto.title, dto.posterPath, dto.backdropPath, dto.overview, dto.voteAverage, dto.releaseDate)
                    }))
                }
                is Result.Error -> emit(Result.Error(result.throwable))
                else -> {}
            }
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    override fun getTrendingMovies(page: Int): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getTrending().toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { dto ->
                Movie(dto.id, dto.title, dto.posterPath, dto.backdropPath, dto.overview, dto.voteAverage, dto.releaseDate)
            }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTopRatedMovies(page: Int): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getTopRated(page).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { dto ->
                Movie(dto.id, dto.title, dto.posterPath, dto.backdropPath, dto.overview, dto.voteAverage, dto.releaseDate)
            }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getMovieDetail(id: Int): Flow<Result<Movie>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getMovieDetail(id).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.let { dto ->
                Movie(dto.id, dto.title, dto.posterPath, dto.backdropPath, dto.overview, dto.voteAverage, dto.releaseDate)
            }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getCredits(id: Int): Flow<Result<List<Cast>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getCredits(id).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.cast.map { Cast(it.id, it.name, it.character, it.profilePath) }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getTrailers(id: Int): Flow<Result<List<Trailer>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getVideos(id).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { Trailer(it.key, it.name, it.site, it.type) }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    override fun getSimilarMovies(id: Int, page: Int): Flow<Result<List<Movie>>> = safeFlow {
        emit(Result.Loading)
        val result = movieService.getSimilar(id, page).toResult()
        when (result) {
            is Result.Success -> emit(Result.Success(result.data.results.map { dto ->
                Movie(dto.id, dto.title, dto.posterPath, dto.backdropPath, dto.overview, dto.voteAverage, dto.releaseDate)
            }))
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }

    private fun MovieEntity.toDomain() = Movie(id, title, posterPath, backdropPath, overview, voteAverage, releaseDate)
    private fun com.sceneseek.tmdb.api.dto.MovieDto.toEntity() = MovieEntity(id, title, overview, posterPath, backdropPath, voteAverage, releaseDate)
}
