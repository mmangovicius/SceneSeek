package com.sceneseek.core.util

enum class ImageSize(val value: String) {
    W185("w185"),
    W342("w342"),
    W500("w500"),
    W780("w780"),
    ORIGINAL("original"),
}

object TmdbImageUrlBuilder {
    private var baseUrl: String = "https://image.tmdb.org/t/p/"

    fun buildUrl(path: String?, size: ImageSize): String? {
        if (path == null) return null
        return "$baseUrl${size.value}$path"
    }

    fun setBaseUrl(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
    }
}
