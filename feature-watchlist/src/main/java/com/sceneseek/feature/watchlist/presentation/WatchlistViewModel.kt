package com.sceneseek.feature.watchlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sceneseek.core.domain.model.WatchlistItem
import com.sceneseek.core.domain.repository.WatchlistRepository
import com.sceneseek.feature.watchlist.domain.usecase.ToggleWatchlistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchlistState(
    val items: List<WatchlistItem> = emptyList(),
    val isEmpty: Boolean = false,
    val isLoading: Boolean = true,
)

sealed class WatchlistNavEvent {
    data class NavigateToDetail(val mediaId: Int, val mediaType: String) : WatchlistNavEvent()
}

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val watchlistRepository: WatchlistRepository,
    private val toggleWatchlist: ToggleWatchlistUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(WatchlistState())
    val state: StateFlow<WatchlistState> = _state.asStateFlow()

    private val _navEvents = Channel<WatchlistNavEvent>(Channel.BUFFERED)
    val navEvents: Flow<WatchlistNavEvent> = _navEvents.receiveAsFlow()

    init {
        watchlistRepository.getAll()
            .onEach { items ->
                _state.update { WatchlistState(items = items, isEmpty = items.isEmpty(), isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onItemClicked(item: WatchlistItem) {
        viewModelScope.launch {
            _navEvents.send(
                WatchlistNavEvent.NavigateToDetail(
                    mediaId = item.mediaId,
                    mediaType = item.mediaType.key,
                )
            )
        }
    }

    fun onItemRemoved(item: WatchlistItem) {
        viewModelScope.launch {
            toggleWatchlist(item)
        }
    }
}
