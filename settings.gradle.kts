pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "SceneSeek"

include(":app")
include(":core")
include(":uicore")
include(":tmdb-api")
include(":tmdb-impl")
include(":movie-storage")
include(":database-impl")
include(":feature-home")
include(":feature-detail")
include(":feature-search")
include(":feature-watchlist")
include(":testutils")
