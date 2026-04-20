package com.sceneseek.feature.detail.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sceneseek.core.domain.model.Cast
import com.sceneseek.core.util.ImageSize
import com.sceneseek.core.util.TmdbImageUrlBuilder
import com.sceneseek.core.domain.model.MediaItem
import com.sceneseek.core.domain.model.Trailer
import com.sceneseek.uicore.components.ErrorState
import com.sceneseek.uicore.components.MediaCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int, String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                is DetailNavEvent.TrailerClicked -> {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url))
                    context.startActivity(intent)
                }
                is DetailNavEvent.NavigateToDetail -> onNavigateToDetail(event.mediaId, event.mediaType)
            }
        }
    }

    val title = state.movie?.title ?: state.tvShow?.name ?: ""
    val backdropPath = state.movie?.backdropPath ?: state.tvShow?.backdropPath
    val overview = state.movie?.overview ?: state.tvShow?.overview ?: ""
    val voteAverage = state.movie?.voteAverage ?: state.tvShow?.voteAverage ?: 0.0
    val year = (state.movie?.releaseDate ?: state.tvShow?.firstAirDate ?: "").take(4)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onWatchlistToggled) {
                        Icon(
                            if (state.isWatchlisted) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (state.isWatchlisted) "Remove from watchlist" else "Add to watchlist",
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator()
            }
            state.error != null -> ErrorState(
                message = state.error ?: "Unknown error",
                onRetry = {},
                modifier = Modifier.padding(paddingValues),
            )
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Backdrop
                item {
                    AsyncImage(
                        model = TmdbImageUrlBuilder.buildUrl(backdropPath, ImageSize.W780),
                        contentDescription = "Backdrop for $title",
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                        contentScale = ContentScale.Crop,
                    )
                }
                // Title + metadata
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = title, style = MaterialTheme.typography.headlineMedium)
                        Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = year, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "★ ${"%.1f".format(voteAverage)}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(text = overview, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                // Cast
                if (state.cast.isNotEmpty()) {
                    item { SectionHeader("Cast") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(state.cast) { cast ->
                                CastItem(cast = cast)
                            }
                        }
                    }
                }
                // Trailers
                if (state.trailers.isNotEmpty()) {
                    item { SectionHeader("Trailers") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.trailers) { trailer ->
                                TrailerCard(trailer = trailer, onClick = { viewModel.onTrailerClicked(trailer) })
                            }
                        }
                    }
                }
                // Similar
                if (state.similar.isNotEmpty()) {
                    item { SectionHeader("Similar") }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.similar) { item ->
                                val (posterPath2, simTitle, rating2) = when (item) {
                                    is MediaItem.MovieItem -> Triple(item.movie.posterPath, item.movie.title, item.movie.voteAverage)
                                    is MediaItem.TvItem -> Triple(item.tvShow.posterPath, item.tvShow.name, item.tvShow.voteAverage)
                                }
                                MediaCard(posterPath = posterPath2, title = simTitle, voteAverage = rating2,
                                    onClick = { viewModel.onSimilarItemClicked(item) }, modifier = Modifier.width(120.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun CastItem(cast: Cast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        AsyncImage(model = TmdbImageUrlBuilder.buildUrl(cast.profilePath, ImageSize.W185),
            contentDescription = cast.name,
            modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Text(text = cast.name, style = MaterialTheme.typography.labelSmall, maxLines = 2)
        Text(text = cast.character, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun TrailerCard(trailer: Trailer, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(160.dp)) {
        Box {
            AsyncImage(model = "https://img.youtube.com/vi/${trailer.key}/mqdefault.jpg",
                contentDescription = trailer.name, modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f),
                contentScale = ContentScale.Crop)
            Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.align(Alignment.Center).size(40.dp))
        }
        Text(text = trailer.name, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp), maxLines = 2)
    }
}
