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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.PaginatedList
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.feature.search.R
import com.sceneseek.uicore.components.EmptyState
import com.sceneseek.uicore.components.ErrorState
import com.sceneseek.uicore.components.PosterImage
import com.sceneseek.uicore.theme.SceneSeekTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToDetail: (Int, String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is SearchNavEvent.NavigateToDetail -> onNavigateToDetail(
                    event.mediaId,
                    event.mediaType,
                )
            }
        }
    }

    SearchContent(
        state = state,
        onQueryChanged = viewModel::onQueryChanged,
        onItemClick = viewModel::onItemClicked,
        onLoadMore = viewModel::loadMore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SearchContent(
    state: SearchState,
    onQueryChanged: (String) -> Unit = {},
    onItemClick: (MediaItem) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = state.query,
            onQueryChange = onQueryChanged,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {}

        when {
            state.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> ErrorState(
                message = state.error ?: stringResource(R.string.search_unknown_error),
                onRetry = { onQueryChanged(state.query) },
            )

            state.isEmpty -> EmptyState(message = stringResource(R.string.search_no_results, state.query))
            else -> {
                val listState = rememberLazyListState()
                LaunchedEffect(listState) {
                    snapshotFlow {
                        val layoutInfo = listState.layoutInfo
                        val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = layoutInfo.totalItemsCount
                        lastVisible to totalItems
                    }.collect { (lastVisible, totalItems) ->
                        if (totalItems > 0 && lastVisible >= totalItems - PREFETCH_DISTANCE) {
                            onLoadMore()
                        }
                    }
                }
                LazyColumn(state = listState) {
                    items(state.results.items) { item ->
                        MediaListItem(item = item, onClick = { onItemClick(item) })
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaListItem(item: MediaItem, onClick: () -> Unit) {
    val movieLabel = stringResource(R.string.search_media_type_movie)
    val tvLabel = stringResource(R.string.search_media_type_tv)
    val (id, title, posterPath, year, typeLabel) = when (item) {
        is MediaItem.MovieItem -> MediaListItemData(
            id = item.movie.id,
            title = item.movie.title,
            posterPath = item.movie.posterPath,
            year = item.movie.releaseDate.take(4),
            typeLabel = movieLabel,
        )

        is MediaItem.TvItem -> MediaListItemData(
            id = item.tvShow.id,
            title = item.tvShow.name,
            posterPath = item.tvShow.posterPath,
            year = item.tvShow.firstAirDate.take(4),
            typeLabel = tvLabel,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(
            path = posterPath,
            modifier = Modifier.size(60.dp, 90.dp),
            contentDescription = stringResource(R.string.search_item_description, title, typeLabel),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Row {
                AssistChip(
                    onClick = {},
                    label = { Text(year) },
                )
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(typeLabel) },
                )
            }
        }
    }
}

private const val PREFETCH_DISTANCE = 3

private data class MediaListItemData(
    val id: Int,
    val title: String,
    val posterPath: String?,
    val year: String,
    val typeLabel: String,
)

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    val sampleItems = listOf(
        MediaItem.MovieItem(
            Movie(
                id = 1,
                title = "The Matrix",
                posterPath = null,
                backdropPath = null,
                overview = "",
                voteAverage = 8.7,
                releaseDate = "1999-03-31",
            )
        ),
        MediaItem.TvItem(
            TvShow(
                id = 2,
                name = "Breaking Bad",
                posterPath = null,
                backdropPath = null,
                overview = "",
                voteAverage = 9.5,
                firstAirDate = "2008-01-20",
            )
        ),
    )
    SceneSeekTheme {
        SearchContent(
            state = SearchState(query = "matrix", results = PaginatedList(sampleItems)),
        )
    }
}
