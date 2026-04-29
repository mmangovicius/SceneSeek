package com.sceneseek.feature.detail.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.model.WatchlistItem
import com.sceneseek.core.domain.repository.WatchlistRepository
import com.sceneseek.core.domain.util.Result
import com.sceneseek.feature.detail.domain.usecase.GetCreditsUseCase
import com.sceneseek.feature.detail.domain.usecase.GetMovieDetailUseCase
import com.sceneseek.feature.detail.domain.usecase.GetSimilarUseCase
import com.sceneseek.feature.detail.domain.usecase.GetTrailersUseCase
import com.sceneseek.feature.detail.domain.usecase.GetTvDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailState(
    val movie: Movie? = null,
    val tvShow: TvShow? = null,
    val cast: List<Cast> = emptyList(),
    val trailers: List<Trailer> = emptyList(),
    val similar: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isWatchlisted: Boolean = false,
)

sealed class DetailNavEvent {
    data class TrailerClicked(val url: String) : DetailNavEvent()
    data class NavigateToDetail(val mediaId: Int, val mediaType: String) : DetailNavEvent()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val getTvDetail: GetTvDetailUseCase,
    private val getCredits: GetCreditsUseCase,
    private val getTrailers: GetTrailersUseCase,
    private val getSimilar: GetSimilarUseCase,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    private val mediaId: Int = checkNotNull(savedStateHandle["mediaId"])
    private val mediaTypeStr: String = checkNotNull(savedStateHandle["mediaType"])
    private val mediaType: MediaType = MediaType.fromKey(mediaTypeStr)

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _navEvents = Channel<DetailNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<DetailNavEvent> = _navEvents.receiveAsFlow()

    init {
        loadDetail()
        observeWatchlist()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (mediaType) {
                is MediaType.Movie -> getMovieDetail(mediaId).collect { result ->
                    when (result) {
                        is Result.Success -> _state.update { it.copy(movie = result.data, isLoading = false) }
                        is Result.Error -> _state.update { it.copy(error = result.throwable.message, isLoading = false) }
                        else -> {}
                    }
                }
                is MediaType.TvShow -> getTvDetail(mediaId).collect { result ->
                    when (result) {
                        is Result.Success -> _state.update { it.copy(tvShow = result.data, isLoading = false) }
                        is Result.Error -> _state.update { it.copy(error = result.throwable.message, isLoading = false) }
                        else -> {}
                    }
                }
            }
        }
        viewModelScope.launch {
            getCredits(mediaId, mediaType).collect { result ->
                if (result is Result.Success) _state.update { it.copy(cast = result.data) }
            }
        }
        viewModelScope.launch {
            getTrailers(mediaId, mediaType).collect { result ->
                if (result is Result.Success) _state.update { it.copy(trailers = result.data) }
            }
        }
        viewModelScope.launch {
            getSimilar(mediaId, mediaType).collect { result ->
                if (result is Result.Success) _state.update { it.copy(similar = result.data) }
            }
        }
    }

    private fun observeWatchlist() {
        viewModelScope.launch {
            watchlistRepository.isWatchlisted(mediaId, mediaType).collect { isWatchlisted ->
                _state.update { it.copy(isWatchlisted = isWatchlisted) }
            }
        }
    }

    fun onWatchlistToggled() {
        viewModelScope.launch {
            val state = _state.value
            val title = state.movie?.title ?: state.tvShow?.name ?: ""
            val posterPath = state.movie?.posterPath ?: state.tvShow?.posterPath
            val item = WatchlistItem(
                mediaId = mediaId,
                mediaType = mediaType,
                title = title,
                posterPath = posterPath,
                addedAt = System.currentTimeMillis(),
            )
            watchlistRepository.toggle(item)
        }
    }

    fun onTrailerClicked(trailer: Trailer) {
        viewModelScope.launch {
            val url = "https://www.youtube.com/watch?v=${trailer.key}"
            _navEvents.send(DetailNavEvent.TrailerClicked(url))
        }
    }

    fun onSimilarItemClicked(item: MediaItem) {
        viewModelScope.launch {
            when (item) {
                is MediaItem.MovieItem -> _navEvents.send(
                    DetailNavEvent.NavigateToDetail(
                        mediaId = item.movie.id,
                        mediaType = MediaType.KEY_MOVIE,
                    )
                )
                is MediaItem.TvItem -> _navEvents.send(
                    DetailNavEvent.NavigateToDetail(
                        mediaId = item.tvShow.id,
                        mediaType = MediaType.KEY_TV,
                    )
                )
            }
        }
    }

    fun onRetry() {
        _state.update { it.copy(isLoading = true, error = null) }
        loadDetail()
    }
}
