package com.sceneseek.tmdb.impl.repository

import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.repository.SearchRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.tmdb.api.service.TmdbSearchService
import com.sceneseek.tmdb.impl.util.toResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

private fun <T> safeFlow(block: suspend kotlinx.coroutines.flow.FlowCollector<Result<T>>.() -> Unit): Flow<Result<T>> =
    flow(block).catch { e -> emit(Result.Error(e)) }

class RemoteSearchRepository @Inject constructor(
    private val searchService: TmdbSearchService,
) : SearchRepository {

    override fun search(query: String, page: Int): Flow<Result<List<MediaItem>>> = safeFlow {
        emit(Result.Loading)
        val result = searchService.searchMulti(query, page).toResult()
        when (result) {
            is Result.Success -> {
                val items = result.data.results.mapNotNull { dto ->
                    when (dto.mediaType) {
                        MediaType.KEY_MOVIE -> MediaItem.MovieItem(Movie(
                            dto.id, dto.title ?: "", dto.posterPath, null,
                            dto.overview ?: "", dto.voteAverage ?: 0.0, dto.releaseDate ?: ""
                        ))
                        MediaType.KEY_TV -> MediaItem.TvItem(TvShow(
                            dto.id, dto.name ?: "", dto.posterPath, null,
                            dto.overview ?: "", dto.voteAverage ?: 0.0, dto.firstAirDate ?: ""
                        ))
                        else -> null // skip person
                    }
                }
                emit(Result.Success(items))
            }
            is Result.Error -> emit(Result.Error(result.throwable))
            else -> {}
        }
    }
}
