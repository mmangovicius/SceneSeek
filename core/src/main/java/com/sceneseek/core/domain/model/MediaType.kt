package com.sceneseek.core.domain.model

sealed class MediaType {
    object Movie : MediaType()
    object TvShow : MediaType()
}
