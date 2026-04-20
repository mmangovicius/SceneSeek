package com.sceneseek.feature.search.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.uicore.components.EmptyState
import com.sceneseek.uicore.components.ErrorState
import com.sceneseek.uicore.components.PosterImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToDetail: (Int, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChanged,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search movies and TV shows…") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {}

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> ErrorState(
                message = state.error ?: "Unknown error",
                onRetry = { viewModel.onQueryChanged(state.query) },
            )
            state.isEmpty -> EmptyState(message = "No results for \"${state.query}\"")
            else -> LazyColumn {
                items(state.items) { item ->
                    MediaListItem(item = item, onClick = {
                        when (item) {
                            is MediaItem.MovieItem -> onNavigateToDetail(item.movie.id, "movie")
                            is MediaItem.TvItem -> onNavigateToDetail(item.tvShow.id, "tv")
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun MediaListItem(item: MediaItem, onClick: () -> Unit) {
    val (id, title, posterPath, year, typeLabel) = when (item) {
        is MediaItem.MovieItem -> MediaListItemData(
            item.movie.id, item.movie.title, item.movie.posterPath,
            item.movie.releaseDate.take(4), "Movie"
        )
        is MediaItem.TvItem -> MediaListItemData(
            item.tvShow.id, item.tvShow.name, item.tvShow.posterPath,
            item.tvShow.firstAirDate.take(4), "TV"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(path = posterPath, modifier = Modifier.size(60.dp, 90.dp), contentDescription = "$title ($typeLabel)")
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Row {
                AssistChip(onClick = {}, label = { Text(year) })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text(typeLabel) })
            }
        }
    }
}

private data class MediaListItemData(
    val id: Int, val title: String, val posterPath: String?,
    val year: String, val typeLabel: String,
)
