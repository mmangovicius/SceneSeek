package com.sceneseek.uicore.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.sceneseek.core.util.ImageSize
import com.sceneseek.core.util.TmdbImageUrlBuilder

@Composable
fun PosterImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
) {
    val imageUrl = TmdbImageUrlBuilder.buildUrl(path, ImageSize.W342)
    SubcomposeAsyncImage(
        model = imageUrl,
        contentDescription = contentDescription.ifEmpty { null },
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            ShimmerEffect(modifier = Modifier.fillMaxSize())
        },
        error = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ShimmerEffect(modifier = Modifier.fillMaxSize())
            }
        },
    )
}
