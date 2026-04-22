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
import com.sceneseek.core.domain.model.TvShow
import com.sceneseek.uicore.components.MediaCard
import com.sceneseek.uicore.components.ShimmerEffect
import com.sceneseek.uicore.theme.SceneSeekTheme
import androidx.compose.ui.tooling.preview.Preview

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
                    event.mediaType
                )
            }
        }
    }

    HomeContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onItemClick = viewModel::onItemClicked,
        onRetry = viewModel::onRetry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onItemClick: (MediaItem) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("SceneSeek") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = onRetry,
            modifier = Modifier.padding(paddingValues),
        ) {
            if (state.isLoading && state.trending.isEmpty()) {
                ShimmerHomeContent()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        ContentRow(
                            title = "Trending",
                            items = state.trending.map { MediaItem.MovieItem(it) },
                            onItemClick = onItemClick,
                        )
                    }
                    item {
                        ContentRow(
                            title = "Popular Movies",
                            items = state.popularMovies.map { MediaItem.MovieItem(it) },
                            onItemClick = onItemClick,
                        )
                    }
                    item {
                        ContentRow(
                            title = "Popular TV",
                            items = state.popularTv.map { MediaItem.TvItem(it) },
                            onItemClick = onItemClick,
                        )
                    }
                    item {
                        ContentRow(
                            title = "Top Rated Movies",
                            items = state.topRatedMovies.map { MediaItem.MovieItem(it) },
                            onItemClick = onItemClick,
                        )
                    }
                    item {
                        ContentRow(
                            title = "Top Rated TV",
                            items = state.topRatedTv.map { MediaItem.TvItem(it) },
                            onItemClick = onItemClick,
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
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                val (id, posterPath, title2, rating, mediaType) = when (item) {
                    is MediaItem.MovieItem -> MediaCardData(
                        item.movie.id,
                        item.movie.posterPath,
                        item.movie.title,
                        item.movie.voteAverage,
                        MediaType.KEY_MOVIE,
                    )

                    is MediaItem.TvItem -> MediaCardData(
                        item.tvShow.id,
                        item.tvShow.posterPath,
                        item.tvShow.name,
                        item.tvShow.voteAverage,
                        MediaType.KEY_TV,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(4) { ShimmerEffect(modifier = Modifier.width(120.dp)) }
                }
            }
        }
    }
}

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
                trending = listOf(sampleMovie, sampleMovie.copy(id = 2, title = "Inception")),
                popularMovies = listOf(sampleMovie.copy(id = 3, title = "Interstellar")),
                popularTv = listOf(sampleTv),
                topRatedMovies = listOf(sampleMovie),
                topRatedTv = listOf(sampleTv.copy(id = 2, name = "The Wire")),
            ),
        )
    }
}
