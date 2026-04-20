package com.sceneseek.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.core.domain.util.Result
import com.sceneseek.feature.home.domain.usecase.GetPopularMoviesUseCase
import com.sceneseek.feature.home.domain.usecase.GetPopularTvUseCase
import com.sceneseek.feature.home.domain.usecase.GetTopRatedMoviesUseCase
import com.sceneseek.feature.home.domain.usecase.GetTopRatedTvUseCase
import com.sceneseek.feature.home.domain.usecase.GetTrendingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val trending: List<Movie> = emptyList(),
    val popularMovies: List<Movie> = emptyList(),
    val popularTv: List<TvShow> = emptyList(),
    val topRatedMovies: List<Movie> = emptyList(),
    val topRatedTv: List<TvShow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class HomeNavEvent {
    data class NavigateToDetail(val mediaId: Int, val mediaType: String) : HomeNavEvent()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTrending: GetTrendingUseCase,
    private val getPopularMovies: GetPopularMoviesUseCase,
    private val getPopularTv: GetPopularTvUseCase,
    private val getTopRatedMovies: GetTopRatedMoviesUseCase,
    private val getTopRatedTv: GetTopRatedTvUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState(isLoading = true))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    private val _navEvents = Channel<HomeNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<HomeNavEvent> = _navEvents.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        loadContent()
    }

    private fun loadContent() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getTrending(),
                getPopularMovies(),
                getPopularTv(),
                getTopRatedMovies(),
                getTopRatedTv(),
            ) { trending, popularMovies, popularTv, topRatedMovies, topRatedTv ->
                val trendingData = if (trending is Result.Success) trending.data else emptyList()
                val popularMoviesData = if (popularMovies is Result.Success) popularMovies.data else emptyList()
                val popularTvData = if (popularTv is Result.Success) popularTv.data else emptyList()
                val topMoviesData = if (topRatedMovies is Result.Success) topRatedMovies.data else emptyList()
                val topTvData = if (topRatedTv is Result.Success) topRatedTv.data else emptyList()
                val results = listOf(trending, popularMovies, popularTv, topRatedMovies, topRatedTv)
                val isStillLoading = results.any { it is Result.Loading }
                val hasError = results.any { it is Result.Error }
                HomeState(
                    trending = trendingData,
                    popularMovies = popularMoviesData,
                    popularTv = popularTvData,
                    topRatedMovies = topMoviesData,
                    topRatedTv = topTvData,
                    isLoading = isStillLoading,
                    error = if (hasError) "Failed to load some content" else null,
                )
            }.collect { newState ->
                _state.value = newState
            }
        }
    }

    fun onItemClicked(item: MediaItem) {
        viewModelScope.launch {
            when (item) {
                is MediaItem.MovieItem -> _navEvents.send(HomeNavEvent.NavigateToDetail(item.movie.id, MediaType.KEY_MOVIE))
                is MediaItem.TvItem -> _navEvents.send(HomeNavEvent.NavigateToDetail(item.tvShow.id, MediaType.KEY_TV))
            }
        }
    }

    fun onRetry() {
        _state.update { it.copy(isLoading = true, error = null) }
        loadContent()
    }
}
