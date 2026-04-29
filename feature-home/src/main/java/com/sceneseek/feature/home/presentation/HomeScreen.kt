package com.sceneseek.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sceneseek.feature.home.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.MediaType
import com.sceneseek.core.domain.model.Movie
import com.sceneseek.core.domain.model.PaginatedList
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.uicore.components.MediaCard
import com.sceneseek.uicore.components.ShimmerEffect
import com.sceneseek.uicore.theme.SceneSeekTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (Int, String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val errorMessage = stringResource(R.string.home_error_load_content)
    LaunchedEffect(state.hasError) {
        if (state.hasError) snackbarHostState.showSnackbar(errorMessage)
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is HomeNavEvent.NavigateToDetail -> onNavigateToDetail(
                    event.mediaId,
                    event.mediaType,
                )
            }
        }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onItemClick = viewModel::onItemClicked,
        onRetry = viewModel::onRetry,
        onLoadMore = viewModel::loadMore,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onItemClick: (MediaItem) -> Unit = {},
    onRetry: () -> Unit = {},
    onLoadMore: (HomeCategory) -> Unit = {},
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.home_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRetry,
            modifier = Modifier.padding(paddingValues),
        ) {
            if (state.isLoading && state.trending.items.isEmpty()) {
                ShimmerHomeContent()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        ContentRow(
                            title = stringResource(R.string.home_section_trending),
                            items = state.trending.items.map { MediaItem.MovieItem(it) },
                            onItemClick = onItemClick,
                            canLoadMore = state.trending.canLoadMore,
                            onLoadMore = { onLoadMore(HomeCategory.TRENDING) },
                        )
                    }
                    item {
                        ContentRow(
                            title = stringResource(R.string.home_section_popular_movies),
                            items = state.popularMovies.items.map { MediaItem.MovieItem(it) },
                            onItemClick = onItemClick,
                            canLoadMore = state.popularMovies.canLoadMore,
                            onLoadMore = { onLoadMore(HomeCategory.POPULAR_MOVIES) },
                        )
                    }
                    item {
                        ContentRow(
                            title = stringResource(R.string.home_section_popular_tv),
                            items = state.popularTv.items.map { MediaItem.TvItem(it) },
                            onItemClick = onItemClick,
                            canLoadMore = state.popularTv.canLoadMore,
                            onLoadMore = { onLoadMore(HomeCategory.POPULAR_TV) },
                        )
                    }
                    item {
                        ContentRow(
                            title = stringResource(R.string.home_section_top_rated_movies),
                            items = state.topRatedMovies.items.map { MediaItem.MovieItem(it) },
                            onItemClick = onItemClick,
                            canLoadMore = state.topRatedMovies.canLoadMore,
                            onLoadMore = { onLoadMore(HomeCategory.TOP_RATED_MOVIES) },
                        )
                    }
                    item {
                        ContentRow(
                            title = stringResource(R.string.home_section_top_rated_tv),
                            items = state.topRatedTv.items.map { MediaItem.TvItem(it) },
                            onItemClick = onItemClick,
                            canLoadMore = state.topRatedTv.canLoadMore,
                            onLoadMore = { onLoadMore(HomeCategory.TOP_RATED_TV) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    canLoadMore: Boolean = true,
    onLoadMore: () -> Unit = {},
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            Pair(lastVisible, totalItems)
        }.collect { (lastVisible, totalItems) ->
            if (totalItems > 0 && lastVisible >= totalItems - PREFETCH_DISTANCE && canLoadMore) {
                onLoadMore()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                val (id, posterPath, title2, rating, mediaType) = when (item) {
                    is MediaItem.MovieItem -> MediaCardData(
                        id = item.movie.id,
                        posterPath = item.movie.posterPath,
                        title = item.movie.title,
                        voteAverage = item.movie.voteAverage,
                        mediaType = MediaType.KEY_MOVIE,
                    )

                    is MediaItem.TvItem -> MediaCardData(
                        id = item.tvShow.id,
                        posterPath = item.tvShow.posterPath,
                        title = item.tvShow.name,
                        voteAverage = item.tvShow.voteAverage,
                        mediaType = MediaType.KEY_TV,
                    )
                }
                MediaCard(
                    posterPath = posterPath,
                    title = title2,
                    voteAverage = rating,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

@Composable
private fun ShimmerHomeContent() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(3) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ShimmerEffect(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.4f))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(4) { ShimmerEffect(modifier = Modifier.width(120.dp)) }
                }
            }
        }
    }
}

private const val PREFETCH_DISTANCE = 3

private data class MediaCardData(
    val id: Int,
    val posterPath: String?,
    val title: String,
    val voteAverage: Double,
    val mediaType: String
)

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val sampleMovie = Movie(
        id = 1,
        title = "The Matrix",
        posterPath = null,
        backdropPath = null,
        overview = "",
        voteAverage = 8.7,
        releaseDate = "1999-03-31",
    )
    val sampleTv = TvShow(
        id = 1,
        name = "Breaking Bad",
        posterPath = null,
        backdropPath = null,
        overview = "",
        voteAverage = 9.5,
        firstAirDate = "2008-01-20",
    )
    SceneSeekTheme {
        HomeContent(
            state = HomeState(
                trending = PaginatedList(listOf(sampleMovie, sampleMovie.copy(id = 2, title = "Inception"))),
                popularMovies = PaginatedList(listOf(sampleMovie.copy(id = 3, title = "Interstellar"))),
                popularTv = PaginatedList(listOf(sampleTv)),
                topRatedMovies = PaginatedList(listOf(sampleMovie)),
                topRatedTv = PaginatedList(listOf(sampleTv.copy(id = 2, name = "The Wire"))),
            ),
        )
    }
}
