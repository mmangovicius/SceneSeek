package com.sceneseek.feature.watchlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sceneseek.core.domain.model.WatchlistItem
import com.sceneseek.uicore.components.EmptyState
import com.sceneseek.uicore.components.PosterImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onNavigateToDetail: (Int, String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is WatchlistNavEvent.NavigateToDetail -> onNavigateToDetail(event.mediaId, event.mediaType)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Watchlist") }) }
    ) { paddingValues ->
        when {
            state.isEmpty -> EmptyState(
                message = "No saved titles yet",
                icon = Icons.Default.Favorite,
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
            )
            else -> LazyColumn(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                items(state.items, key = { it.mediaId }) { item ->
                    SwipeToDismissItem(
                        item = item,
                        onDismiss = { viewModel.onItemRemoved(item) },
                        onClick = { viewModel.onItemClicked(item) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissItem(
    item: WatchlistItem,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.targetValue == SwipeToDismissBoxValue.Settled) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.error
            }
            Box(
                modifier = Modifier.fillMaxSize().background(color).padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White)
            }
        },
        content = {
            WatchlistItemRow(item = item, onClick = onClick)
        }
    )
}

@Composable
private fun WatchlistItemRow(item: WatchlistItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(path = item.posterPath, modifier = Modifier.size(48.dp, 72.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when (item.mediaType) {
                    is com.sceneseek.core.domain.model.MediaType.Movie -> "Movie"
                    is com.sceneseek.core.domain.model.MediaType.TvShow -> "TV Show"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
