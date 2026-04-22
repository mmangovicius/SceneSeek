package com.sceneseek.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.PaginatedList
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

enum class HomeCategory {
    TRENDING, POPULAR_MOVIES, POPULAR_TV, TOP_RATED_MOVIES, TOP_RATED_TV
}

data class HomeState(
    val trending: PaginatedList<Movie> = PaginatedList(),
    val popularMovies: PaginatedList<Movie> = PaginatedList(),
    val popularTv: PaginatedList<TvShow> = PaginatedList(),
    val topRatedMovies: PaginatedList<Movie> = PaginatedList(),
    val topRatedTv: PaginatedList<TvShow> = PaginatedList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
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
                val popularMoviesData =
                    if (popularMovies is Result.Success) popularMovies.data else emptyList()
                val popularTvData = if (popularTv is Result.Success) popularTv.data else emptyList()
                val topMoviesData =
                    if (topRatedMovies is Result.Success) topRatedMovies.data else emptyList()
                val topTvData = if (topRatedTv is Result.Success) topRatedTv.data else emptyList()
                val results = listOf(trending, popularMovies, popularTv, topRatedMovies, topRatedTv)
                val isStillLoading = results.any { it is Result.Loading }
                val hasError = results.any { it is Result.Error }
                HomeState(
                    trending = PaginatedList(items = trendingData),
                    popularMovies = PaginatedList(items = popularMoviesData),
                    popularTv = PaginatedList(items = popularTvData),
                    topRatedMovies = PaginatedList(items = topMoviesData),
                    topRatedTv = PaginatedList(items = topTvData),
                    isLoading = isStillLoading,
                    hasError = hasError,
                )
            }.collect { newState ->
                _state.update { newState }
            }
        }
    }

    fun loadMore(category: HomeCategory) {
        val state = _state.value
        when (category) {
            HomeCategory.TRENDING -> {
                loadMoreItems(
                    state.trending,
                    { p -> getTrending(p) }
                ) { state, pl -> state.copy(trending = pl) }
            }

            HomeCategory.POPULAR_MOVIES -> {
                loadMoreItems(
                    state.popularMovies,
                    { p -> getPopularMovies(p) }
                ) { state, pl -> state.copy(popularMovies = pl) }
            }

            HomeCategory.POPULAR_TV -> {
                loadMoreItems(
                    state.popularTv,
                    { p -> getPopularTv(p) }
                ) { state, pl -> state.copy(popularTv = pl) }
            }

            HomeCategory.TOP_RATED_MOVIES -> {
                loadMoreItems(
                    state.topRatedMovies,
                    { p -> getTopRatedMovies(p) }
                ) { state, pl -> state.copy(topRatedMovies = pl) }
            }

            HomeCategory.TOP_RATED_TV -> {
                loadMoreItems(
                    state.topRatedTv,
                    { p -> getTopRatedTv(p) }
                ) { state, pl -> state.copy(topRatedTv = pl) }
            }
        }
    }

    private fun <T> loadMoreItems(
        current: PaginatedList<T>,
        fetch: (Int) -> Flow<Result<List<T>>>,
        copyState: (HomeState, PaginatedList<T>) -> HomeState,
    ) {
        if (!current.canLoadMore) return
        val nextPage = current.page + 1
        viewModelScope.launch {
            fetch(nextPage).collect { result ->
                when (result) {
                    is Result.Success -> _state.update {
                        copyState(
                            it,
                            PaginatedList(
                                current.items + result.data,
                                nextPage,
                                result.data.isNotEmpty()
                            )
                        )
                    }

                    is Result.Error -> _state.update {
                        copyState(it, current.copy(canLoadMore = false))
                    }

                    else -> Unit
                }
            }
        }
    }

    fun onItemClicked(item: MediaItem) {
        viewModelScope.launch {
            when (item) {
                is MediaItem.MovieItem -> {
                    _navEvents.send(
                        HomeNavEvent.NavigateToDetail(
                            item.movie.id,
                            MediaType.KEY_MOVIE
                        )
                    )
                }

                is MediaItem.TvItem -> {
                    _navEvents.send(
                        HomeNavEvent.NavigateToDetail(
                            item.tvShow.id,
                            MediaType.KEY_TV
                        )
                    )
                }
            }
        }
    }

    fun onRetry() {
        _state.update { it.copy(isLoading = true, hasError = false) }
        loadContent()
    }
}
