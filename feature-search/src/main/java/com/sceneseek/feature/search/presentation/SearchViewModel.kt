package com.sceneseek.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.PaginatedList
import com.sceneseek.core.domain.repository.SearchRepository
import com.sceneseek.core.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: PaginatedList<MediaItem> = PaginatedList(),
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val error: String? = null,
    val isLoadingMore: Boolean = false,
)

sealed class SearchNavEvent {
    data class NavigateToDetail(val mediaId: Int, val mediaType: String) : SearchNavEvent()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _navEvents = Channel<SearchNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<SearchNavEvent> = _navEvents.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")

    init {
        observeQuery()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeQuery() {
        queryFlow
            .debounce(300L)
            .filter { it.length > 1 }
            .flatMapLatest { query -> searchRepository.search(query = query, page = 1) }
            .onEach { result ->
                when (result) {
                    is Result.Loading -> _state.update { it.copy(isLoading = true, error = null) }
                    is Result.Success -> _state.update {
                        it.copy(
                            isLoading = false,
                            results = PaginatedList(result.data),
                            isEmpty = result.data.isEmpty(),
                            error = null,
                        )
                    }
                    is Result.Error -> _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.throwable.message,
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _state.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun onQueryCleared() {
        _state.update { SearchState() }
        queryFlow.value = ""
    }

    fun onItemClicked(item: MediaItem) {
        viewModelScope.launch {
            when (item) {
                is MediaItem.MovieItem -> _navEvents.send(
                    SearchNavEvent.NavigateToDetail(
                        mediaId = item.movie.id,
                        mediaType = MediaType.KEY_MOVIE,
                    )
                )
                is MediaItem.TvItem -> _navEvents.send(
                    SearchNavEvent.NavigateToDetail(
                        mediaId = item.tvShow.id,
                        mediaType = MediaType.KEY_TV,
                    )
                )
            }
        }
    }

    fun loadMore() {
        val state = _state.value
        if (state.isLoadingMore || !state.results.canLoadMore || state.query.length <= 1) return
        val nextPage = state.results.page + 1
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            searchRepository.search(query = state.query, page = nextPage).collect { result ->
                when (result) {
                    is Result.Success -> _state.update {
                        it.copy(
                            results = PaginatedList(
                                items = it.results.items + result.data,
                                page = nextPage,
                                canLoadMore = result.data.isNotEmpty(),
                            ),
                            isLoadingMore = false,
                        )
                    }
                    is Result.Error -> _state.update {
                        it.copy(
                            results = it.results.copy(canLoadMore = false),
                            isLoadingMore = false,
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
